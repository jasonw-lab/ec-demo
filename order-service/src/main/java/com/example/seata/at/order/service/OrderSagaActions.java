package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.order.client.PaymentClient;
import com.example.seata.at.order.client.StorageClient;
import com.example.seata.at.order.client.dto.PaymentResult;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.OrderStatus;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import com.example.seata.at.order.infrastructure.messaging.kafka.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

/**
 * Order Saga Actions: Actions called by Seata Saga state machine (order_initialization_saga).
 * 
 * This service contains ONLY methods invoked during the Saga execution phase:
 * - initOrderPending: Create order in CREATED state
 * - reserveStock: Reserve inventory
 * - requestPayment: Request payment from PayPay
 * - releaseStock: Compensation for reserveStock
 * - markCancelled: Compensation for order creation
 * 
 * Methods NOT in this class (moved to OrderStateManager):
 * - markPaid: Called from Webhook (outside Saga)
 * - commitStock: Called from Webhook (outside Saga)
 * 
 * Design principle: Clear separation between Saga-managed actions and post-Saga async processing.
 */
@Service("orderSagaActions")
public class OrderSagaActions {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaActions.class);
    private static final Duration PAYMENT_WAIT_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration CHANNEL_TOKEN_REFRESH_THRESHOLD = Duration.ofMinutes(1);
    private final OrderMapper orderMapper;
    private final StorageClient storageClient;
    private final PaymentClient paymentClient;
    private final OrderEventPublisher eventPublisher;
    private final OrderStateManager stateManager;

    public OrderSagaActions(OrderMapper orderMapper, StorageClient storageClient, PaymentClient paymentClient, 
                           OrderEventPublisher eventPublisher, OrderStateManager stateManager) {
        this.orderMapper = orderMapper;
        this.storageClient = storageClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
        this.stateManager = stateManager;
    }

    @Transactional
    public boolean initOrderPending(String orderNo, Long userId, Long productId, Integer count, BigDecimal amount) {
        require(orderNo, "orderNo");
        require(userId, "userId");
        require(productId, "productId");
        require(count, "count");
        require(amount, "amount");

        log.info("[SAGA] initOrderPending orderNo={} userId={} productId={} count={} amount={}",
                orderNo, userId, productId, count, amount);

        Order existing = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (existing != null) {
            log.info("[SAGA] initOrderPending order exists orderNo={} status={}", orderNo, existing.getStatus());
            return true;
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setCount(count);
        order.setAmount(amount);
        order.setStatus(OrderStatus.CREATED.name());
        order.setPaymentStatus(null);
        order.setPaymentUrl(null);
        order.setPaymentRequestedAt(null);
        order.setPaymentExpiresAt(null);
        order.setPaymentCompletedAt(null);
        order.setPaymentChannelToken(null);
        order.setPaymentChannelExpiresAt(null);
        order.setPaymentLastEventId(null);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        int inserted = orderMapper.insert(order);
        log.info("[SAGA] initOrderPending created orderNo={} inserted={}", orderNo, inserted);
        if (inserted == 1) {
            // Order Service → Kafka: publish status change event for downstream notifications (BFF WS).
            log.info("[SAGA][Kafka] publish OrderStatusChanged orderNo={} oldStatus={} newStatus={}",
                    orderNo, null, OrderStatus.CREATED.name());
            eventPublisher.publishStatusChanged(order, null, OrderStatus.CREATED.name(), null, null, null);
        }
        return inserted == 1;
    }

    /**
     * Reserve stock (Saga action: called during order_initialization_saga).
     */
    public boolean reserveStock(String orderNo, Long productId, Integer count) {
        log.info("[SAGA] reserveStock orderNo={} productId={} count={}", orderNo, productId, count);
        return storageClient.reserveStock(orderNo, productId, count);
    }

    /**
     * Release stock (Saga compensation: called if Saga fails after reserveStock).
     */
    public boolean releaseStock(String orderNo, Long productId, Integer count) {
        log.info("[SAGA] releaseStock orderNo={} productId={} count={}", orderNo, productId, count);
        return stateManager.releaseStock(orderNo, productId, count);
    }

    @Transactional
    public boolean requestPayment(String orderNo, BigDecimal amount) {
        require(orderNo, "orderNo");
        require(amount, "amount");
        log.info("[SAGA] requestPayment orderNo={} amount={}", orderNo, amount);

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.warn("[SAGA] requestPayment cannot find order orderNo={}", orderNo);
            return false;
        }
        OrderStatus status = OrderStatus.fromValue(order.getStatus());
        if (OrderStatus.PAID.equals(status)) {
            log.info("[SAGA] requestPayment order already paid orderNo={}", orderNo);
            return true;
        }
        if (OrderStatus.CANCELLED.equals(status)) {
            log.warn("[SAGA] requestPayment order already cancelled orderNo={}", orderNo);
            return false;
        }
        if (OrderStatus.PAYMENT_PENDING.equals(status)) {
            log.info("[SAGA] requestPayment already pending orderNo={}", orderNo);
            return true;
        }

        PaymentResult result = paymentClient.requestPayment(orderNo, amount);
        if (result == null || !result.isSuccess()) {
            String code = result == null ? "NO_RESULT" : firstNonBlank(result.getCode(), "PAYPAY_ERROR");
            String message = result == null ? "No response from payment backend" : firstNonBlank(result.getMessage(), "PayPay payment request failed");
            log.warn("[SAGA] requestPayment failed orderNo={} code={} message={}", orderNo, code, message);
            markCancelled(orderNo, code, message);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = resolveExpiry(result.getExpiresAt(), now.plus(PAYMENT_WAIT_TIMEOUT));
        String paypayStatus = normalizeStatus(result.getStatus());
        String resolvedPaymentStatus = firstNonBlank(paypayStatus, "PENDING");
        String channelToken = ensureChannelToken(order, now);
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.CREATED.name(), OrderStatus.PAYMENT_PENDING.name())
                .set(Order::getStatus, OrderStatus.PAYMENT_PENDING.name())
                .set(Order::getPaymentStatus, resolvedPaymentStatus)
                .set(Order::getPaymentUrl, preferredUrl(result))
                .set(Order::getPaymentRequestedAt, now)
                .set(Order::getPaymentExpiresAt, expiresAt)
                .set(Order::getPaymentChannelToken, channelToken)
                .set(Order::getPaymentChannelExpiresAt, expiresAt)
                .set(Order::getPaymentLastEventId, null)
                .set(Order::getUpdateTime, now);
        int updated = orderMapper.update(null, uw);
        log.info("[SAGA] requestPayment updated={} orderNo={} expiresAt={} channelToken={}", updated, orderNo, expiresAt, channelToken);
        if (updated > 0) {
            // Order Service → Kafka: publish status change event for downstream notifications (BFF WS).
            log.info("[SAGA][Kafka] publish OrderStatusChanged orderNo={} oldStatus={} newStatus={} paymentStatus={}",
                    orderNo, order.getStatus(), OrderStatus.PAYMENT_PENDING.name(), resolvedPaymentStatus);
            eventPublisher.publishStatusChanged(order, order.getStatus(), OrderStatus.PAYMENT_PENDING.name(), resolvedPaymentStatus, null, null);
        }
        return updated > 0;
    }

    /**
     * Mark order as cancelled (Saga compensation: called when Saga fails).
     * Delegates to OrderStateManager for actual state transition.
     */
    public boolean markCancelled(String orderNo, String failCode, String failMessage) {
        log.info("[SAGA] markCancelled (delegating to StateManager) orderNo={} code={} message={}", orderNo, failCode, failMessage);
        return stateManager.transitionToCancelled(orderNo, failCode, failMessage);
    }

    private String ensureChannelToken(Order order, LocalDateTime now) {
        if (order != null && StringUtils.hasText(order.getPaymentChannelToken())) {
            LocalDateTime currentExpiry = order.getPaymentChannelExpiresAt();
            if (currentExpiry != null && currentExpiry.isAfter(now.plus(CHANNEL_TOKEN_REFRESH_THRESHOLD))) {
                return order.getPaymentChannelToken();
            }
        }
        return generateChannelToken();
    }

    private static String generateChannelToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static LocalDateTime resolveExpiry(String expiresAt, LocalDateTime fallback) {
        if (!StringUtils.hasText(expiresAt)) {
            return fallback;
        }
        String value = expiresAt.trim();
        try {
            Instant instant = Instant.parse(value);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (DateTimeParseException ignored) {
            try {
                long epoch = Long.parseLong(value);
                Instant instant = epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
                return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            } catch (NumberFormatException ignored2) {
                return fallback;
            }
        }
    }

    private static String preferredUrl(PaymentResult result) {
        return firstNonBlank(result.getDeeplink(), result.getPaymentUrl());
    }

    private static String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static void require(Object value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
    }
}
