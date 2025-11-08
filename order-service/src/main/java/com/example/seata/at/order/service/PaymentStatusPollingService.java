package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seata.at.order.api.dto.PaymentStatusUpdateRequest;
import com.example.seata.at.order.client.PaymentClient;
import com.example.seata.at.order.client.dto.PaymentResult;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.OrderStatus;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * PayPay支払いステータスのポーリングサービス
 * 
 * <p>Webhookが来ない場合のフォールバックとして、定期的にPayPay APIを呼び出して
 * 支払いステータスをチェックし、完了を検知したら自動的に注文ステータスを更新します。
 * 
 * <p>Webhookが優先されるため、このポーリングは補完的な役割を果たします。
 */
@Service
public class PaymentStatusPollingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusPollingService.class);
    
    /** 支払い成功とみなすステータス */
    private static final Set<String> SUCCESS_STATUSES = Set.of("COMPLETED", "SUCCESS", "CAPTURED");
    
    /** 支払い失敗とみなすステータス */
    private static final Set<String> FAILURE_STATUSES = Set.of("FAILED", "FAILURE", "DECLINED", "CANCELED", "CANCELLED");
    
    /** タイムアウトとみなすステータス */
    private static final Set<String> TIMEOUT_STATUSES = Set.of("TIMED_OUT", "EXPIRED");
    
    /** 一度に処理する最大注文数（API負荷軽減のため） */
    private static final int MAX_ORDERS_PER_BATCH = 10;

    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;
    private final OrderPaymentService orderPaymentService;

    public PaymentStatusPollingService(OrderMapper orderMapper,
                                      PaymentClient paymentClient,
                                      OrderPaymentService orderPaymentService) {
        this.orderMapper = orderMapper;
        this.paymentClient = paymentClient;
        this.orderPaymentService = orderPaymentService;
    }

    /**
     * 待機中の支払いについて、PayPay APIを呼び出してステータスをチェックし、
     * 完了または失敗を検知したら注文ステータスを更新します。
     * 
     * <p>処理対象：
     * - ステータスが WAITING_PAYMENT の注文
     * - 支払い有効期限がまだ切れていない注文
     * 
     * <p>一度に処理する注文数は制限されています（MAX_ORDERS_PER_BATCH）。
     * 
     * @return 更新された注文数
     */
    @Transactional
    public int checkAndUpdatePaymentStatus() {
        LocalDateTime now = LocalDateTime.now();
        
        // 有効期限内のWAITING_PAYMENT状態の注文を取得
        List<Order> waitingOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.WAITING_PAYMENT.name())
                .ge(Order::getPaymentExpiresAt, now)
                .last("LIMIT " + MAX_ORDERS_PER_BATCH));

        if (waitingOrders.isEmpty()) {
            log.debug("[PaymentPolling] No waiting orders to check");
            return 0;
        }

        log.info("[PaymentPolling] Starting payment status check for {} orders", waitingOrders.size());
        int updatedCount = 0;
        int errorCount = 0;

        for (Order order : waitingOrders) {
            String orderNo = order.getOrderNo();
            try {
                boolean updated = checkSingleOrder(order);
                if (updated) {
                    updatedCount++;
                }
            } catch (Exception e) {
                errorCount++;
                log.error("[PaymentPolling] Failed to check payment status for orderNo={}, error={}", 
                         orderNo, e.getMessage(), e);
            }
        }

        log.info("[PaymentPolling] Payment status check completed: checked={}, updated={}, errors={}", 
                waitingOrders.size(), updatedCount, errorCount);
        
        return updatedCount;
    }

    /**
     * 単一の注文についてPayPay APIを呼び出し、ステータスをチェックして更新します。
     * 
     * @param order チェック対象の注文
     * @return 更新が行われた場合true
     */
    private boolean checkSingleOrder(Order order) {
        String orderNo = order.getOrderNo();
        
        log.debug("[PaymentPolling] Checking payment status for orderNo={}", orderNo);
        
        // PayPay APIを呼び出してステータスを取得
        PaymentResult result = paymentClient.getStatus(orderNo);
        
        if (result == null) {
            log.debug("[PaymentPolling] Payment status check returned null for orderNo={}", orderNo);
            return false;
        }

        String status = result.getStatus();
        if (!StringUtils.hasText(status)) {
            log.debug("[PaymentPolling] Payment status is empty for orderNo={}, success={}", 
                     orderNo, result.isSuccess());
            return false;
        }

        String normalized = status.trim().toUpperCase();
        log.info("[PaymentPolling] PayPay status for orderNo={}: {} (success={})", 
                orderNo, normalized, result.isSuccess());

        // ステータスに応じて注文を更新
        if (SUCCESS_STATUSES.contains(normalized)) {
            return handleSuccessStatus(orderNo, normalized, result);
        } else if (FAILURE_STATUSES.contains(normalized) || TIMEOUT_STATUSES.contains(normalized)) {
            return handleFailureStatus(orderNo, normalized, result);
        } else {
            log.debug("[PaymentPolling] Payment status is still pending for orderNo={}, status={}", 
                     orderNo, normalized);
            return false;
        }
    }

    /**
     * 支払い成功ステータスを処理します。
     * 
     * @param orderNo 注文番号
     * @param normalized 正規化されたステータス
     * @param result PayPay APIレスポンス
     * @return 更新が行われた場合true
     */
    private boolean handleSuccessStatus(String orderNo, String normalized, PaymentResult result) {
        log.info("[PaymentPolling] Payment completed detected orderNo={}, status={}", orderNo, normalized);
        
        try {
            PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
            request.setStatus(normalized);
            request.setCode(result.getCode());
            request.setMessage(result.getMessage());
            // ポーリングのためeventIdは設定しない（Webhookと区別するため）
            
            orderPaymentService.handlePaymentStatus(orderNo, request);
            
            log.info("[PaymentPolling] Order status updated to PAID via polling orderNo={}, status={}", 
                    orderNo, normalized);
            return true;
        } catch (Exception e) {
            log.error("[PaymentPolling] Failed to update order to PAID orderNo={}, error={}", 
                     orderNo, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 支払い失敗ステータスを処理します。
     * 
     * @param orderNo 注文番号
     * @param normalized 正規化されたステータス
     * @param result PayPay APIレスポンス
     * @return 更新が行われた場合true
     */
    private boolean handleFailureStatus(String orderNo, String normalized, PaymentResult result) {
        log.info("[PaymentPolling] Payment failed detected orderNo={}, status={}", orderNo, normalized);
        
        try {
            PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
            request.setStatus(normalized);
            request.setCode(result.getCode());
            request.setMessage(result.getMessage());
            // ポーリングのためeventIdは設定しない（Webhookと区別するため）
            
            orderPaymentService.handlePaymentStatus(orderNo, request);
            
            log.info("[PaymentPolling] Order status updated to FAILED via polling orderNo={}, status={}", 
                    orderNo, normalized);
            return true;
        } catch (Exception e) {
            log.error("[PaymentPolling] Failed to update order to FAILED orderNo={}, error={}", 
                     orderNo, e.getMessage(), e);
            throw e;
        }
    }
}

