package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.OrderStatus;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderPaymentTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentTimeoutScheduler.class);

    private final OrderMapper orderMapper;
    private final OrderSagaActions orderSagaActions;

    public OrderPaymentTimeoutScheduler(OrderMapper orderMapper, OrderSagaActions orderSagaActions) {
        this.orderMapper = orderMapper;
        this.orderSagaActions = orderSagaActions;
    }

    @Scheduled(fixedDelayString = "${order.payment.timeout-check-interval-ms:60000}")
    @Transactional
    public void enforcePaymentTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.WAITING_PAYMENT.name())
                .lt(Order::getPaymentExpiresAt, now));

        for (Order order : expired) {
            String orderNo = order.getOrderNo();
            log.info("[PaymentTimeout] detected expired payment orderNo={} expiresAt={}", orderNo, order.getPaymentExpiresAt());
            boolean updated = orderSagaActions.markFailed(orderNo, "PAYPAY_TIMEOUT", "PayPay payment timed out");
            if (updated) {
                orderSagaActions.storageCompensate(orderNo, order.getProductId(), order.getCount());
            }
        }
    }
}
