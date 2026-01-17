package com.demo.ec.order.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.ec.order.web.dto.PaymentStatusUpdateRequest;
import com.demo.ec.order.gateway.client.PaymentClient;
import com.demo.ec.order.gateway.client.dto.PaymentResult;
import com.demo.ec.order.domain.Order;
import com.demo.ec.order.domain.OrderStatus;
import com.demo.ec.order.gateway.OrderMapper;
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
 * <p>Webhookが来ない場合のフォールバックとして、定期的にPayment Service経由で
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
     * <p>重要な設計判断：
     * - スケジューラーで定期的に実行されるため、例外処理が重要
     * - 一つの注文でエラーが発生しても、他の注文の処理は継続
     * - バッチサイズを制限してAPI負荷とDB負荷を制御
     * - トランザクションは各注文ごとに分離（一つの失敗が他に影響しない）
     * 
     * <p>処理対象：
     * - ステータスが PAYMENT_PENDING の注文
     * - 支払い有効期限がまだ切れていない注文
     * 
     * <p>一度に処理する注文数は制限されています（MAX_ORDERS_PER_BATCH）。
     * 
     * @return 更新された注文数
     */
    @Transactional
    public int checkAndUpdatePaymentStatus() {
        LocalDateTime now = LocalDateTime.now();
        
        // 有効期限内のPAYMENT_PENDING状態の注文を取得
        // ■ LIMIT句でバッチサイズを制限し、APIとDBの負荷を抑える
        List<Order> waitingOrders = findWaitingPaymentOrders(now);

        if (waitingOrders.isEmpty()) {
            log.debug("[PaymentPolling] No waiting orders to check");
            return 0;
        }

        log.info("[PaymentPolling] Starting payment status check for {} orders", waitingOrders.size());
        int updatedCount = 0;
        int errorCount = 0;

        // 各注文を個別に処理（一つの失敗が他に影響しない）
        for (Order order : waitingOrders) {
            String orderNo = order.getOrderNo();
            try {
                boolean updated = checkSingleOrder(order);
                if (updated) {
                    updatedCount++;
                }
            } catch (Exception e) {
                errorCount++;
                // ■ 一つの注文でエラーが発生しても他の注文の処理は続行
                // スケジューラーが停止すると全体の監視が止まるため
                log.error("[PaymentPolling] Failed to check payment status for orderNo={}, error={}", 
                         orderNo, e.getMessage(), e);
            }
        }

        // 統計ログ（監視とデバッグに有用）
        if (updatedCount > 0 || errorCount > 0) {
            log.info("[PaymentPolling] Payment status check completed: checked={}, updated={}, errors={}", 
                    waitingOrders.size(), updatedCount, errorCount);
        }
        
        return updatedCount;
    }

    /**
     * 待機中の支払い注文を取得します。
     * 
     * <p>設計判断：
     * - メソッドを分離することで、テスト容易性を向上
     * - バッチサイズの制限を明確化
     * 
     * @param now 現在時刻（非null保証済み）
     * @return 待機中の注文リスト（非null保証、空の可能性あり）
     */
    private List<Order> findWaitingPaymentOrders(LocalDateTime now) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PAYMENT_PENDING.name())
                .ge(Order::getPaymentExpiresAt, now)
                .last("LIMIT " + MAX_ORDERS_PER_BATCH));
    }

    /**
     * 単一の注文についてPayPay APIを呼び出し、ステータスをチェックして更新します。
     * 
     * <p>設計判断：
     * - null安全性を確保（APIレスポンスがnullの場合の処理）
     * - ステータスの正規化（大文字変換、トリム）を統一
     * - エラーハンドリングは呼び出し元に委譲
     * 
     * @param order チェック対象の注文（非null保証済み）
     * @return 更新が行われた場合true
     * @throws Exception PayPay API呼び出しまたは注文更新でエラーが発生した場合
     */
    private boolean checkSingleOrder(Order order) throws Exception {
        String orderNo = order.getOrderNo();
        
        // null安全性チェック
        if (!StringUtils.hasText(orderNo)) {
            log.warn("[PaymentPolling] Order has empty orderNo, orderId={}", order.getId());
            return false;
        }
        
        log.debug("[PaymentPolling] Checking payment status for orderNo={}", orderNo);
        
        // PayPay APIを呼び出してステータスを取得
        // 注意: 外部API呼び出しのため、ネットワークエラーやタイムアウトの可能性がある
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

        // ステータスの正規化（一貫性のため）
        String normalized = normalizeStatus(status);
        String code = result.getCode() != null && StringUtils.hasText(result.getCode()) 
                ? result.getCode().trim().toUpperCase() : null;
        boolean isSuccess = result.isSuccess();
        
        log.info("[PaymentPolling] PayPay status for orderNo={}: {} (success={}, code={})", 
                orderNo, normalized, isSuccess, code);

        // 支払い成功の判定:
        // 1. statusがSUCCESS_STATUSESに含まれる
        // 2. または success=true かつ code="OK" (BFFからのレスポンス)
        // 一回成功取得後はポーリングを停止するため、成功時は必ず注文ステータスを更新
        boolean isPaymentSuccess = SUCCESS_STATUSES.contains(normalized) 
                || (isSuccess && "OK".equals(code));
        
        // 支払い失敗の判定:
        // 1. statusがFAILURE_STATUSESまたはTIMEOUT_STATUSESに含まれる
        // 2. または success=false かつ codeが失敗を示す
        boolean isPaymentFailure = FAILURE_STATUSES.contains(normalized) 
                || TIMEOUT_STATUSES.contains(normalized)
                || (!isSuccess && code != null && !"PENDING".equals(code) && !"OK".equals(code));

        // ステータスに応じて注文を更新
        if (isPaymentSuccess) {
            // 成功時はステータスを"COMPLETED"に正規化して処理
            String successStatus = SUCCESS_STATUSES.contains(normalized) ? normalized : "COMPLETED";
            return handleSuccessStatus(orderNo, successStatus, result);
        } else if (isPaymentFailure) {
            // 失敗時はステータスを正規化して処理
            String failureStatus = FAILURE_STATUSES.contains(normalized) || TIMEOUT_STATUSES.contains(normalized)
                    ? normalized : "FAILED";
            return handleFailureStatus(orderNo, failureStatus, result);
        } else {
            log.debug("[PaymentPolling] Payment status is still pending for orderNo={}, status={}, success={}, code={}", 
                     orderNo, normalized, isSuccess, code);
            return false;
        }
    }

    /**
     * ステータス文字列を正規化します。
     * 
     * <p>設計判断：
     * - 大文字変換とトリムを統一して適用
     * - null安全性を確保
     * 
     * @param status 元のステータス（null可）
     * @return 正規化されたステータス（null可）
     */
    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    /**
     * 支払い成功ステータスを処理します。
     * 
     * <p>設計判断：
     * - ポーリングのためeventIdは設定しない（Webhookと区別するため）
     * - エラーは呼び出し元に伝播（トランザクションロールバックのため）
     * 
     * @param orderNo 注文番号（非null保証済み）
     * @param normalized 正規化されたステータス（非null保証済み）
     * @param result PayPay APIレスポンス（非null保証済み）
     * @return 更新が行われた場合true
     * @throws Exception 注文更新でエラーが発生した場合
     */
    private boolean handleSuccessStatus(String orderNo, String normalized, PaymentResult result) throws Exception {
        log.info("[PaymentPolling] Payment completed detected orderNo={}, status={}", orderNo, normalized);
        
        // PaymentStatusUpdateRequestを構築
        // ■ ポーリングなのでeventIdは設定せずにWebhookと区別する
        PaymentStatusUpdateRequest request = buildPaymentStatusRequest(normalized, result);
        
        // 注文ステータスを更新（トランザクション内で実行）
        // 注意: このメソッド内でトランザクションが開始される
        orderPaymentService.handlePaymentStatus(orderNo, request);
        
        log.info("[PaymentPolling] Order status updated to PAID via polling orderNo={}, status={}", 
                orderNo, normalized);
        return true;
    }

    /**
     * 支払い失敗ステータスを処理します。
     * 
     * <p>設計判断：
     * - ポーリングのためeventIdは設定しない（Webhookと区別するため）
     * - エラーは呼び出し元に伝播（トランザクションロールバックのため）
     * 
     * @param orderNo 注文番号（非null保証済み）
     * @param normalized 正規化されたステータス（非null保証済み）
     * @param result PayPay APIレスポンス（非null保証済み）
     * @return 更新が行われた場合true
     * @throws Exception 注文更新でエラーが発生した場合
     */
    private boolean handleFailureStatus(String orderNo, String normalized, PaymentResult result) throws Exception {
        log.info("[PaymentPolling] Payment failed detected orderNo={}, status={}", orderNo, normalized);
        
        // PaymentStatusUpdateRequestを構築
        // ■ ポーリングなのでeventIdは設定せずにWebhookと区別する
        PaymentStatusUpdateRequest request = buildPaymentStatusRequest(normalized, result);
        
        // 注文ステータスを更新（トランザクション内で実行）
        orderPaymentService.handlePaymentStatus(orderNo, request);
        
        log.info("[PaymentPolling] Order status updated to CANCELLED via polling orderNo={}, status={}",
                orderNo, normalized);
        return true;
    }

    /**
     * PaymentStatusUpdateRequestを構築します。
     * 
     * <p>設計判断：
     * - 重複コードを削減（DRY原則）
     * - ポーリングのためeventIdは設定しない（Webhookと区別するため）
     * 
     * @param normalized 正規化されたステータス（非null保証済み）
     * @param result PayPay APIレスポンス（非null保証済み）
     * @return 構築されたリクエスト（非null保証）
     */
    private PaymentStatusUpdateRequest buildPaymentStatusRequest(String normalized, PaymentResult result) {
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
        request.setStatus(normalized);
        request.setCode(result.getCode());
        request.setMessage(result.getMessage());
        // ■ ポーリングなのでeventIdは設定せずにWebhookと区別する
        return request;
    }
}
