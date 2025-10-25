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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service("orderSagaActions")
public class OrderSagaActions {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaActions.class);

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

    public boolean doPayment(String orderNo, BigDecimal amount) {
        log.info("[SAGA] doPayment orderNo={} amount={}", orderNo, amount);
        PaymentResult result = paymentClient.pay(orderNo, amount);
        boolean success = result != null && result.isSuccess();
        if (!success) {
            log.warn("[SAGA] doPayment failed orderNo={} code={} message={}",
                    orderNo,
                    result == null ? "NO_RESULT" : result.getCode(),
                    result == null ? "null" : result.getMessage());
        }
        return success;
    }

    @Transactional
    public boolean markPaid(String orderNo) {
        log.info("[SAGA] markPaid orderNo={}", orderNo);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.PENDING.name(), OrderStatus.PAID.name())
                .set(Order::getStatus, OrderStatus.PAID.name())
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
                .in(Order::getStatus, OrderStatus.PENDING.name(), OrderStatus.FAILED.name())
                .set(Order::getStatus, OrderStatus.FAILED.name())
                .set(Order::getFailCode, failCode)
                .set(Order::getFailMessage, failMessage)
                .set(Order::getPaidAt, null)
                .set(Order::getFailedAt, now)
                .set(Order::getUpdateTime, now);
        int updated = orderMapper.update(null, uw);
        log.info("[SAGA] markFailed updated={} orderNo={}", updated, orderNo);
        return updated > 0;
    }

    private static void require(Object value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
    }
}

