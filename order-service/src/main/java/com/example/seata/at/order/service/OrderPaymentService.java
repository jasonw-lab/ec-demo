package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.order.api.dto.PaymentStatusUpdateRequest;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.OrderStatus;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Service
public class OrderPaymentService {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentService.class);

    private static final Set<String> SUCCESS_STATUSES = Set.of("COMPLETED", "SUCCESS", "CAPTURED");
    private static final Set<String> FAILURE_STATUSES = Set.of("FAILED", "FAILURE", "DECLINED", "CANCELED", "CANCELLED");
    private static final Set<String> TIMEOUT_STATUSES = Set.of("TIMED_OUT", "EXPIRED");

    private final OrderMapper orderMapper;
    private final OrderSagaActions orderSagaActions;

    public OrderPaymentService(OrderMapper orderMapper, OrderSagaActions orderSagaActions) {
        this.orderMapper = orderMapper;
        this.orderSagaActions = orderSagaActions;
    }

    @Transactional
    public Order handlePaymentStatus(String orderNo, PaymentStatusUpdateRequest request) {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        String normalized = request.normalizedStatus();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("status must not be blank");
        }

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new IllegalArgumentException("Order not found for orderNo=" + orderNo);
        }

        if (request.hasEventId() && request.getEventId().equals(order.getPaymentLastEventId())) {
            log.info("[PaymentCallback] duplicate event ignored orderNo={} eventId={}", orderNo, request.getEventId());
            return order;
        }

        OrderStatus currentStatus = OrderStatus.fromValue(order.getStatus());
        LocalDateTime eventTime = request.eventTimeAsLocalDateTime().orElse(null);

        if (SUCCESS_STATUSES.contains(normalized)) {
            if (!OrderStatus.PAID.equals(currentStatus)) {
                log.info("[PaymentCallback] confirming success orderNo={} status={}", orderNo, normalized);
                orderSagaActions.storageConfirm(orderNo, order.getProductId(), order.getCount());
                orderSagaActions.markPaid(orderNo);
            } else {
                log.info("[PaymentCallback] order already PAID orderNo={} ignoring state mutation", orderNo);
            }
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
        } else if (FAILURE_STATUSES.contains(normalized) || TIMEOUT_STATUSES.contains(normalized)) {
            if (OrderStatus.PAID.equals(currentStatus)) {
                log.warn("[PaymentCallback] received failure status after PAID orderNo={} status={} -> ignore", orderNo, normalized);
                updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
                return order;
            }
            String failCode = firstNonBlank(request.getCode(), normalized);
            String failMessage = firstNonBlank(request.getMessage(), "PayPay status " + normalized);
            if (!OrderStatus.FAILED.equals(currentStatus)) {
                log.info("[PaymentCallback] marking order as FAILED orderNo={} status={} code={}", orderNo, normalized, failCode);
                orderSagaActions.markFailed(orderNo, failCode, failMessage);
                orderSagaActions.storageCompensate(orderNo, order.getProductId(), order.getCount());
            } else {
                log.info("[PaymentCallback] order already FAILED orderNo={} ignoring repeated failure event", orderNo);
            }
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, false, failMessage, failCode);
        } else {
            log.warn("[PaymentCallback] unsupported status received orderNo={} status={}", orderNo, normalized);
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, false, request.getMessage(), request.getCode());
        }

        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
    }

    private void updatePaymentMeta(String orderNo,
                                   String paymentStatus,
                                   String eventId,
                                   LocalDateTime eventTime,
                                   boolean success,
                                   String failMessage,
                                   String failCode) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
          .set(Order::getPaymentStatus, paymentStatus)
          .set(Order::getUpdateTime, now);

        if (StringUtils.hasText(eventId)) {
            uw.set(Order::getPaymentLastEventId, eventId);
        }
        if (eventTime != null) {
            uw.set(Order::getPaymentCompletedAt, eventTime);
            if (success) {
                uw.set(Order::getPaidAt, eventTime);
                uw.set(Order::getFailedAt, null);
            } else {
                uw.set(Order::getFailedAt, eventTime);
            }
        }

        if (success) {
            uw.set(Order::getFailCode, null);
            uw.set(Order::getFailMessage, null);
        } else {
            if (StringUtils.hasText(failCode)) {
                uw.set(Order::getFailCode, failCode);
            }
            if (StringUtils.hasText(failMessage)) {
                uw.set(Order::getFailMessage, failMessage);
            }
        }

        orderMapper.update(null, uw);
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
}
