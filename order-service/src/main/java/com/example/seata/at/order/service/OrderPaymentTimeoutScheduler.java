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

/**
 * 注文支払いタイムアウト監視スケジューラー
 * 
 * <p>定期的に実行されるスケジューラーで、以下の処理を行います：
 * <ul>
 *   <li>支払いステータスのポーリング（PaymentStatusPollingServiceに委譲）</li>
 *   <li>タイムアウトした支払いの検出と失敗マーク</li>
 * </ul>
 * 
 * <p>Webhookが優先されますが、Webhookが来ない場合のフォールバックとして
 * ポーリングが動作します。
 */
@Component
public class OrderPaymentTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentTimeoutScheduler.class);

    private final OrderMapper orderMapper;
    private final OrderSagaActions orderSagaActions;
    private final PaymentStatusPollingService paymentStatusPollingService;

    public OrderPaymentTimeoutScheduler(OrderMapper orderMapper,
                                       OrderSagaActions orderSagaActions,
                                       PaymentStatusPollingService paymentStatusPollingService) {
        this.orderMapper = orderMapper;
        this.orderSagaActions = orderSagaActions;
        this.paymentStatusPollingService = paymentStatusPollingService;
    }

    /**
     * PayPayの支払いステータスをポーリングして、完了していたら自動更新
     * 
     * <p>Webhookが来ない場合のフォールバックとして動作します。
     * デフォルトでは10秒ごとに実行されます。
     * 
     * <p>設定: order.payment.status-check-interval-ms (デフォルト: 10000ms)
     */
    @Scheduled(fixedDelayString = "${order.payment.status-check-interval-ms:10000}")
    @Transactional
    public void checkPaymentStatus() {
        try {
            log.debug("[PaymentScheduler] Starting payment status polling");
            int updatedCount = paymentStatusPollingService.checkAndUpdatePaymentStatus();
            if (updatedCount > 0) {
                log.info("[PaymentScheduler] Payment status polling completed: {} orders updated", updatedCount);
            }
        } catch (Exception e) {
            log.error("[PaymentScheduler] Payment status polling failed: {}", e.getMessage(), e);
        }
    }

    /**
     * タイムアウトした支払いを検出してキャンセルとしてマーク
     *
     * <p>支払い有効期限を過ぎた注文を検出し、自動的にキャンセルとしてマークします。
     * 在庫も自動的に解放されます。
     * 
     * <p>デフォルトでは60秒ごとに実行されます。
     * 設定: order.payment.timeout-check-interval-ms (デフォルト: 60000ms)
     */
    @Scheduled(fixedDelayString = "${order.payment.timeout-check-interval-ms:60000}")
    @Transactional
    public void enforcePaymentTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PAYMENT_PENDING.name())
                .lt(Order::getPaymentExpiresAt, now));

        if (expired.isEmpty()) {
            log.debug("[PaymentTimeout] No expired payments found");
            return;
        }

        log.info("[PaymentTimeout] Found {} expired payments", expired.size());
        int updatedCount = 0;
        int errorCount = 0;

        for (Order order : expired) {
            String orderNo = order.getOrderNo();
            try {
                log.info("[PaymentTimeout] Processing expired payment orderNo={}, expiresAt={}", 
                        orderNo, order.getPaymentExpiresAt());
                boolean updated = orderSagaActions.markCancelled(orderNo, "PAYPAY_TIMEOUT", "PayPay payment timed out");
                if (updated) {
                    orderSagaActions.releaseStock(orderNo, order.getProductId(), order.getCount());
                    updatedCount++;
                    log.info("[PaymentTimeout] Order marked as CANCELLED and stock released orderNo={}", orderNo);
                } else {
                    log.warn("[PaymentTimeout] Failed to mark order as CANCELLED orderNo={}", orderNo);
                }
            } catch (Exception e) {
                errorCount++;
                log.error("[PaymentTimeout] Error processing expired payment orderNo={}, error={}", 
                         orderNo, e.getMessage(), e);
            }
        }

        log.info("[PaymentTimeout] Timeout check completed: found={}, updated={}, errors={}", 
                expired.size(), updatedCount, errorCount);
    }
}
