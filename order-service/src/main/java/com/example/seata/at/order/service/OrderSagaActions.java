package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.order.client.PaymentClient;
import com.example.seata.at.order.client.StorageClient;
import com.example.seata.at.order.client.dto.PaymentResult;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.OrderStatus;
import com.example.seata.at.order.domain.mapper.OrderMapper;
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
import java.util.Set;

@Service("orderSagaActions")
public class OrderSagaActions {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaActions.class);
    private static final Duration PAYMENT_WAIT_TIMEOUT = Duration.ofMinutes(15);
    private static final long PAYMENT_STATUS_POLL_INTERVAL_MS = 3_000L;
    private static final Set<String> PAYPAY_SUCCESS_STATUSES = Set.of("COMPLETED", "SUCCESS", "CAPTURED");
    private static final Set<String> PAYPAY_FAILURE_STATUSES = Set.of("FAILED", "CANCELED", "CANCELLED", "EXPIRED", "DECLINED");

    private final OrderMapper orderMapper;
    private final StorageClient storageClient;
    private final PaymentClient paymentClient;

    public OrderSagaActions(OrderMapper orderMapper, StorageClient storageClient, PaymentClient paymentClient) {
        this.orderMapper = orderMapper;
        this.storageClient = storageClient;
        this.paymentClient = paymentClient;
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
        order.setStatus(OrderStatus.PENDING.name());
        order.setPaymentStatus(null);
        order.setPaymentUrl(null);
        order.setPaymentRequestedAt(null);
        order.setPaymentExpiresAt(null);
        order.setPaymentCompletedAt(null);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        int inserted = orderMapper.insert(order);
        log.info("[SAGA] initOrderPending created orderNo={} inserted={}", orderNo, inserted);
        return inserted == 1;
    }

    public boolean storageDeduct(String orderNo, Long productId, Integer count) {
        log.info("[SAGA] storageDeduct orderNo={} productId={} count={}", orderNo, productId, count);
        return storageClient.deduct(orderNo, productId, count);
    }

    public boolean storageCompensate(String orderNo, Long productId, Integer count) {
        log.info("[SAGA] storageCompensate orderNo={} productId={} count={}", orderNo, productId, count);
        return storageClient.compensate(orderNo, productId, count);
    }

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
        if (OrderStatus.FAILED.equals(status)) {
            log.warn("[SAGA] requestPayment order already failed orderNo={}", orderNo);
            return false;
        }
        if (OrderStatus.WAITING_PAYMENT.equals(status)) {
            log.info("[SAGA] requestPayment already waiting orderNo={}", orderNo);
            return true;
        }

        PaymentResult result = paymentClient.requestPayment(orderNo, amount);
        if (result == null || !result.isSuccess()) {
            String code = result == null ? "NO_RESULT" : firstNonBlank(result.getCode(), "PAYPAY_ERROR");
            String message = result == null ? "No response from payment backend" : firstNonBlank(result.getMessage(), "PayPay payment request failed");
            log.warn("[SAGA] requestPayment failed orderNo={} code={} message={}", orderNo, code, message);
            markFailed(orderNo, code, message);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = resolveExpiry(result.getExpiresAt(), now.plus(PAYMENT_WAIT_TIMEOUT));
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.PENDING.name(), OrderStatus.WAITING_PAYMENT.name())
                .set(Order::getStatus, OrderStatus.WAITING_PAYMENT.name())
                .set(Order::getPaymentStatus, normalizeStatus(result.getStatus()))
                .set(Order::getPaymentUrl, preferredUrl(result))
                .set(Order::getPaymentRequestedAt, now)
                .set(Order::getPaymentExpiresAt, expiresAt)
                .set(Order::getUpdateTime, now);
        int updated = orderMapper.update(null, uw);
        log.info("[SAGA] requestPayment updated={} orderNo={} expiresAt={}", updated, orderNo, expiresAt);
        return updated > 0;
    }

    public boolean awaitPaymentResult(String orderNo) {
        require(orderNo, "orderNo");
        log.info("[SAGA] awaitPaymentResult orderNo={}", orderNo);

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.warn("[SAGA] awaitPaymentResult order not found orderNo={}", orderNo);
            return false;
        }

        while (true) {
            Order latest = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderNo));
            if (latest == null) {
                log.warn("[SAGA] awaitPaymentResult order disappeared orderNo={}", orderNo);
                return false;
            }

            OrderStatus currentStatus = OrderStatus.fromValue(latest.getStatus());
            LocalDateTime deadline = determineDeadline(latest);
            if (OrderStatus.PAID.equals(currentStatus)) {
                log.info("[SAGA] awaitPaymentResult already paid orderNo={}", orderNo);
                return true;
            }
            if (OrderStatus.FAILED.equals(currentStatus)) {
                log.info("[SAGA] awaitPaymentResult already failed orderNo={}", orderNo);
                return false;
            }

            if (LocalDateTime.now().isAfter(deadline)) {
                log.warn("[SAGA] awaitPaymentResult timed out orderNo={} deadline={}", orderNo, deadline);
                markFailed(orderNo, "PAYPAY_TIMEOUT", "PayPay payment timed out");
                return false;
            }

            PaymentResult statusResult = paymentClient.getStatus(orderNo);
            if (statusResult != null) {
                updatePaymentSnapshot(orderNo, statusResult);
                String paypayStatus = normalizeStatus(statusResult.getStatus());
                if (paypayStatus != null) {
                    if (PAYPAY_SUCCESS_STATUSES.contains(paypayStatus)) {
                        log.info("[SAGA] awaitPaymentResult PayPay success orderNo={} paypayStatus={}", orderNo, paypayStatus);
                        markPaid(orderNo);
                        return true;
                    }
                    if (PAYPAY_FAILURE_STATUSES.contains(paypayStatus)) {
                        String code = firstNonBlank(statusResult.getCode(), paypayStatus);
                        String message = firstNonBlank(statusResult.getMessage(), "PayPay status " + paypayStatus);
                        log.warn("[SAGA] awaitPaymentResult PayPay failure orderNo={} status={} code={}", orderNo, paypayStatus, code);
                        markFailed(orderNo, code, message);
                        return false;
                    }
                }
            }

            sleepQuietly();
        }
    }

    @Transactional
    public boolean markPaid(String orderNo) {
        log.info("[SAGA] markPaid orderNo={}", orderNo);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.PENDING.name(), OrderStatus.WAITING_PAYMENT.name(), OrderStatus.PAID.name())
                .set(Order::getStatus, OrderStatus.PAID.name())
                .set(Order::getPaymentStatus, "COMPLETED")
                .set(Order::getPaymentCompletedAt, now)
                .set(Order::getPaidAt, now)
                .set(Order::getFailedAt, null)
                .set(Order::getFailCode, null)
                .set(Order::getFailMessage, null)
                .set(Order::getUpdateTime, now);
        int updated = orderMapper.update(null, uw);
        log.info("[SAGA] markPaid updated={} orderNo={}", updated, orderNo);
        return updated > 0;
    }

    @Transactional
    public boolean markFailed(String orderNo, String failCode, String failMessage) {
        log.info("[SAGA] markFailed orderNo={} code={} message={}", orderNo, failCode, failMessage);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.PENDING.name(), OrderStatus.WAITING_PAYMENT.name(), OrderStatus.FAILED.name())
                .set(Order::getStatus, OrderStatus.FAILED.name())
                .set(Order::getFailCode, failCode)
                .set(Order::getFailMessage, failMessage)
                .set(Order::getPaymentUrl, null)
                .set(Order::getPaidAt, null)
                .set(Order::getFailedAt, now)
                .set(Order::getPaymentStatus, "FAILED")
                .set(Order::getPaymentCompletedAt, null)
                .set(Order::getUpdateTime, now);
        int updated = orderMapper.update(null, uw);
        log.info("[SAGA] markFailed updated={} orderNo={}", updated, orderNo);
        return updated > 0;
    }

    private LocalDateTime determineDeadline(Order order) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestedAt = order.getPaymentRequestedAt() != null ? order.getPaymentRequestedAt() : now;
        LocalDateTime expiresAt = order.getPaymentExpiresAt();
        if (expiresAt != null) {
            return expiresAt.isBefore(now) ? now : expiresAt;
        }
        return requestedAt.plus(PAYMENT_WAIT_TIMEOUT);
    }

    private void updatePaymentSnapshot(String orderNo, PaymentResult result) {
        if (result == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .set(Order::getPaymentStatus, normalizeStatus(result.getStatus()))
                .set(Order::getPaymentUrl, preferredUrl(result))
                .set(Order::getUpdateTime, now);
        if (StringUtils.hasText(result.getExpiresAt())) {
            uw.set(Order::getPaymentExpiresAt, resolveExpiry(result.getExpiresAt(), now.plus(PAYMENT_WAIT_TIMEOUT)));
        }
        orderMapper.update(null, uw);
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

    private void sleepQuietly() {
        try {
            Thread.sleep(PAYMENT_STATUS_POLL_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for PayPay status", e);
        }
    }

    private static void require(Object value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
    }
}
