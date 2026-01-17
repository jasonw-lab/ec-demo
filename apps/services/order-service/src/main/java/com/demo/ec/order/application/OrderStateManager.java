package com.demo.ec.order.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.demo.ec.order.gateway.client.StorageClient;
import com.demo.ec.order.domain.Order;
import com.demo.ec.order.domain.OrderStatus;
import com.demo.ec.order.gateway.OrderMapper;
import com.demo.ec.order.gateway.messaging.kafka.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Order State Manager: Manages order state transitions outside of Saga (e.g., Webhook processing).
 * 
 * This service handles state transitions that occur after Saga completion:
 * - Payment success (Webhook) → PAID
 * - Payment failure (Webhook) → CANCELLED
 * - Stock commitment after payment success
 * - Stock release after payment failure
 * 
 * Design principle: Separated from OrderSagaActions to clearly distinguish Saga-managed 
 * transactions from post-Saga asynchronous processing.
 */
@Service
public class OrderStateManager {
    private static final Logger log = LoggerFactory.getLogger(OrderStateManager.class);
    
    private final OrderMapper orderMapper;
    private final StorageClient storageClient;
    private final OrderEventPublisher eventPublisher;

    public OrderStateManager(OrderMapper orderMapper, StorageClient storageClient, OrderEventPublisher eventPublisher) {
        this.orderMapper = orderMapper;
        this.storageClient = storageClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Transition order to PAID state (called from Webhook on payment success).
     * 
     * @param orderNo Order number
     * @return true if update succeeded
     */
    @Transactional
    public boolean transitionToPaid(String orderNo) {
        log.info("[StateManager] transitionToPaid orderNo={}", orderNo);
        Order before = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.CREATED.name(), OrderStatus.PAYMENT_PENDING.name(), OrderStatus.PAID.name())
                .set(Order::getStatus, OrderStatus.PAID.name())
                .set(Order::getPaymentStatus, "COMPLETED")
                .set(Order::getPaymentCompletedAt, now)
                .set(Order::getPaidAt, now)
                .set(Order::getFailedAt, null)
                .set(Order::getFailCode, null)
                .set(Order::getFailMessage, null)
                .set(Order::getPaymentChannelToken, null)
                .set(Order::getPaymentChannelExpiresAt, null)
                .set(Order::getUpdateTime, now);
        int updated = orderMapper.update(null, uw);
        log.info("[StateManager] transitionToPaid updated={} orderNo={}", updated, orderNo);
        if (updated > 0 && before != null) {
            log.info("[StateManager][Kafka] publish OrderStatusChanged orderNo={} oldStatus={} newStatus={} paymentStatus={}",
                    orderNo, before.getStatus(), OrderStatus.PAID.name(), "COMPLETED");
            eventPublisher.publishStatusChanged(before, before.getStatus(), OrderStatus.PAID.name(), "COMPLETED", null, null);
        }
        return updated > 0;
    }

    /**
     * Transition order to CANCELLED state (called from Webhook on payment failure or compensation).
     * 
     * @param orderNo Order number
     * @param failCode Failure code
     * @param failMessage Failure message
     * @return true if update succeeded
     */
    @Transactional
    public boolean transitionToCancelled(String orderNo, String failCode, String failMessage) {
        // Truncate failMessage to DB column limit to avoid DataTruncation exceptions
        if (failMessage != null && failMessage.length() > 255) {
            int originalLen = failMessage.length();
            log.warn("[StateManager] transitionToCancelled failMessage too long ({} chars), truncating to 255 for orderNo={}", originalLen, orderNo);
            failMessage = failMessage.substring(0, 255);
        }
        log.info("[StateManager] transitionToCancelled orderNo={} code={} message={}", orderNo, failCode, failMessage);
        Order before = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.CREATED.name(), OrderStatus.PAYMENT_PENDING.name(), OrderStatus.CANCELLED.name())
                .set(Order::getStatus, OrderStatus.CANCELLED.name())
                .set(Order::getFailCode, failCode)
                .set(Order::getFailMessage, failMessage)
                .set(Order::getPaymentUrl, null)
                .set(Order::getPaidAt, null)
                .set(Order::getFailedAt, now)
                .set(Order::getPaymentStatus, "FAILED")
                .set(Order::getPaymentCompletedAt, null)
                .set(Order::getPaymentChannelToken, null)
                .set(Order::getPaymentChannelExpiresAt, null)
                .set(Order::getUpdateTime, now);
        int updated = orderMapper.update(null, uw);
        log.info("[StateManager] transitionToCancelled updated={} orderNo={}", updated, orderNo);
        if (updated > 0 && before != null) {
            log.info("[StateManager][Kafka] publish OrderStatusChanged orderNo={} oldStatus={} newStatus={} paymentStatus={} reason={}",
                    orderNo, before.getStatus(), OrderStatus.CANCELLED.name(), "FAILED", failMessage);
            eventPublisher.publishStatusChanged(before, before.getStatus(), OrderStatus.CANCELLED.name(), "FAILED", failMessage, null);
        }
        return updated > 0;
    }

    /**
     * Commit stock (called from Webhook on payment success).
     * Moves inventory from RESERVED to COMMITTED state.
     * 
     * @param orderNo Order number
     * @param productId Product ID
     * @param count Quantity
     * @return true if commit succeeded
     */
    public boolean commitStock(String orderNo, Long productId, Integer count) {
        log.info("[StateManager] commitStock orderNo={} productId={} count={}", orderNo, productId, count);
        return storageClient.commitStock(orderNo, productId, count);
    }

    /**
     * Release stock (called from compensation on payment failure).
     * Returns inventory from RESERVED to AVAILABLE state.
     * 
     * @param orderNo Order number
     * @param productId Product ID
     * @param count Quantity
     * @return true if release succeeded
     */
    public boolean releaseStock(String orderNo, Long productId, Integer count) {
        log.info("[StateManager] releaseStock orderNo={} productId={} count={}", orderNo, productId, count);
        return storageClient.releaseStock(orderNo, productId, count);
    }
}
