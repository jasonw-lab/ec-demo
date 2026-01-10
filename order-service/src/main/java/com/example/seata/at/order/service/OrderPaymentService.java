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

/**
 * 注文支払いステータス更新サービス
 * 
 * <p>Webhookまたはポーリングから呼び出され、注文の支払いステータスを更新します。
 * このサービスは共通のビジネスロジックを提供し、Webhookとポーリングの両方から
 * 利用されます。
 * 
 * <p>処理内容：
 * <ul>
 *   <li>支払い成功時の注文ステータス更新（PAID）</li>
 *   <li>支払い失敗時の注文ステータス更新（CANCELLED）</li>
 *   <li>重複イベントの検知と防止</li>
 *   <li>在庫の確定または補償処理</li>
 * </ul>
 */
@Service
public class OrderPaymentService {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentService.class);

    /** 支払い成功とみなすステータス */
    private static final Set<String> SUCCESS_STATUSES = Set.of("COMPLETED", "SUCCESS", "CAPTURED");
    
    /** 支払い失敗とみなすステータス */
    private static final Set<String> FAILURE_STATUSES = Set.of("FAILED", "FAILURE", "DECLINED", "CANCELED", "CANCELLED");
    
    /** タイムアウトとみなすステータス */
    private static final Set<String> TIMEOUT_STATUSES = Set.of("TIMED_OUT", "EXPIRED");

    private final OrderMapper orderMapper;
    private final OrderSagaActions orderSagaActions;

    public OrderPaymentService(OrderMapper orderMapper, OrderSagaActions orderSagaActions) {
        this.orderMapper = orderMapper;
        this.orderSagaActions = orderSagaActions;
    }

    /**
     * 支払いステータス更新リクエストを処理し、注文ステータスを更新します。
     * 
     * <p>Webhookまたはポーリングから呼び出されます。
     * 重複イベントは自動的に検知され、無視されます。
     * 
     * @param orderNo 注文番号
     * @param request 支払いステータス更新リクエスト
     * @return 更新された注文
     * @throws IllegalArgumentException 注文が見つからない場合、またはステータスが無効な場合
     */
    @Transactional
    public Order handlePaymentStatus(String orderNo, PaymentStatusUpdateRequest request) {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        String normalized = request.normalizedStatus();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("status must not be blank");
        }

        log.info("[PaymentService] Processing payment status update orderNo={}, status={}, eventId={}", 
                orderNo, normalized, request.getEventId());

        // 注文の存在確認と取得
        // ■ トランザクション内で実行されるため、この時点での注文状態が保証される
        Order order = findOrderSafely(orderNo);

        // 重複イベントのチェック（Webhookの場合のみ有効）
        if (request.hasEventId() && request.getEventId().equals(order.getPaymentLastEventId())) {
            log.info("[PaymentService] Duplicate event ignored orderNo={}, eventId={}, lastEventId={}", 
                    orderNo, request.getEventId(), order.getPaymentLastEventId());
            return order;
        }

        OrderStatus currentStatus = OrderStatus.fromValue(order.getStatus());
        LocalDateTime eventTime = request.eventTimeAsLocalDateTime().orElse(null);

        log.info("[PaymentService] Current order status orderNo={}, currentStatus={}, newStatus={}", 
                orderNo, currentStatus, normalized);

        // ステータスに応じて処理を分岐
        if (SUCCESS_STATUSES.contains(normalized)) {
            handleSuccessStatus(orderNo, normalized, currentStatus, request, eventTime);
        } else if (FAILURE_STATUSES.contains(normalized) || TIMEOUT_STATUSES.contains(normalized)) {
            handleFailureStatus(orderNo, normalized, currentStatus, request, eventTime);
        } else {
            log.warn("[PaymentService] Unsupported status received orderNo={}, status={}", orderNo, normalized);
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, false, request.getMessage(), request.getCode());
        }

        // 更新後の注文を取得して返す
        // ■ トランザクションコミット前の最新状態を取得する
        Order updated = findOrderSafely(orderNo);
        log.info("[PaymentService] Payment status update completed orderNo={}, finalStatus={}", 
                orderNo, updated.getStatus());
        return updated;
    }

    /**
     * 注文を安全に取得します。
     * 
     * <p>このメソッドは注文の存在確認と取得を一括で行い、
     * 見つからない場合は適切な例外をスローします。
     * 
     * <p>設計判断：
     * - 重複コードを削減（DRY原則）
     * - エラーメッセージの一貫性を保証
     * - nullチェックの漏れを防止
     * 
     * @param orderNo 注文番号（非null保証済み）
     * @return 注文エンティティ（非null保証）
     * @throws IllegalArgumentException 注文が見つからない場合
     */
    private Order findOrderSafely(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.error("[PaymentService] Order not found orderNo={}", orderNo);
            throw new IllegalArgumentException("Order not found for orderNo=" + orderNo);
        }
        return order;
    }

    /**
     * 支払い成功ステータスを処理します。
     * 
     * <p>重要な設計判断：
     * - 既にPAID状態の場合は冪等性を保つため、状態変更をスキップ
     * - 在庫確定と注文ステータス更新は順序が重要（在庫確定→注文更新）
     * - トランザクション内で実行されるため、いずれかが失敗すれば全体がロールバック
     * 
     * @param orderNo 注文番号（非null保証済み）
     * @param normalized 正規化されたステータス（非null保証済み）
     * @param currentStatus 現在の注文ステータス（非null保証済み）
     * @param request 支払いステータス更新リクエスト（非null保証済み）
     * @param eventTime イベント発生時刻（null可）
     */
    private void handleSuccessStatus(String orderNo, 
                                    String normalized, 
                                    OrderStatus currentStatus,
                                    PaymentStatusUpdateRequest request,
                                    LocalDateTime eventTime) {
        // 冪等性チェック: 既にPAID状態の場合は処理をスキップ
        // ■ Webhookの再送やポーリングの重複実行に備えて安全
        if (OrderStatus.PAID.equals(currentStatus)) {
            log.info("[PaymentService] Order already PAID, skipping state mutation orderNo={}", orderNo);
            // メタデータのみ更新（イベントIDの記録など）
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
            return;
        }
        if (OrderStatus.CANCELLED.equals(currentStatus)) {
            log.warn("[PaymentService] Success received after CANCELLED, ignoring state mutation orderNo={}", orderNo);
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
            return;
        }

        log.info("[PaymentService] Confirming payment success orderNo={}, status={}, currentStatus={}", 
                orderNo, normalized, currentStatus);
        
        // 注文情報を再取得（トランザクション内で最新状態を保証）
        // 注意: 並行処理による状態変更の可能性があるため、処理直前に再取得
        Order order = findOrderSafely(orderNo);
        
        // 在庫確定処理（補償トランザクションの逆操作）
        // ■ 在庫確定→注文更新の順で実行する
        orderSagaActions.commitStock(orderNo, order.getProductId(), order.getCount());
        
        // 注文ステータスをPAIDに更新
        orderSagaActions.markPaid(orderNo);
        
        log.info("[PaymentService] Order marked as PAID orderNo={}, productId={}, count={}", 
                orderNo, order.getProductId(), order.getCount());
        
        // 支払いメタデータを更新（イベントID、完了時刻など）
        updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
    }

    /**
     * 支払い失敗ステータスを処理します。
     * 
     * <p>重要な設計判断：
     * - 既にPAID状態の場合は、データ整合性を保つため失敗イベントを無視
     * - 在庫補償は注文更新の前に実行（補償トランザクションの順序）
     * - 失敗情報（コード・メッセージ）はメタデータとして保存
     * 
     * @param orderNo 注文番号（非null保証済み）
     * @param normalized 正規化されたステータス（非null保証済み）
     * @param currentStatus 現在の注文ステータス（非null保証済み）
     * @param request 支払いステータス更新リクエスト（非null保証済み）
     * @param eventTime イベント発生時刻（null可）
     */
    private void handleFailureStatus(String orderNo, 
                                    String normalized, 
                                    OrderStatus currentStatus,
                                    PaymentStatusUpdateRequest request,
                                    LocalDateTime eventTime) {
        // データ整合性チェック: PAID状態の注文に対して失敗イベントが来た場合
        // ■ 決済完了後に失敗イベントが来る可能性（PayPay側のタイミング問題など）に対応
        if (OrderStatus.PAID.equals(currentStatus)) {
            log.warn("[PaymentService] Received failure status after PAID, ignoring to maintain data integrity orderNo={}, status={}", 
                    orderNo, normalized);
            // メタデータのみ更新（イベントの記録は残す）
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
            return;
        }
        
        // 失敗情報の抽出（優先順位: リクエストのコード/メッセージ > ステータス）
        String failCode = firstNonBlank(request.getCode(), normalized);
        String failMessage = firstNonBlank(request.getMessage(), "PayPay status " + normalized);
        
        // 冪等性チェック: 既にCANCELLED状態の場合は処理をスキップ
        if (OrderStatus.CANCELLED.equals(currentStatus)) {
            log.info("[PaymentService] Order already CANCELLED, skipping state mutation orderNo={}", orderNo);
            // メタデータのみ更新（最新の失敗情報を記録）
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, false, failMessage, failCode);
            return;
        }
        
        log.info("[PaymentService] Marking order as CANCELLED orderNo={}, status={}, code={}, message={}",
                orderNo, normalized, failCode, failMessage);
        
        // 注文情報を再取得（トランザクション内で最新状態を保証）
        Order order = findOrderSafely(orderNo);
        
        // 注文ステータスをCANCELLEDに更新
        orderSagaActions.markCancelled(orderNo, failCode, failMessage);
        
        // 在庫補償処理（予約していた在庫を解放）
        // ■ 注文更新→在庫補償の順で実行する（Sagaパターンの補償フロー）
        orderSagaActions.releaseStock(orderNo, order.getProductId(), order.getCount());
        
        log.info("[PaymentService] Order marked as CANCELLED and stock compensated orderNo={}, productId={}, count={}",
                orderNo, order.getProductId(), order.getCount());
        
        // 支払いメタデータを更新（失敗情報、イベントID、完了時刻など）
        updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, false, failMessage, failCode);
    }

    /**
     * 注文の支払いメタデータを更新します。
     * 
     * <p>支払いステータス、イベントID、完了時刻、失敗情報などを更新します。
     * 
     * @param orderNo 注文番号
     * @param paymentStatus 支払いステータス
     * @param eventId イベントID（重複検知用、オプション）
     * @param eventTime イベント発生時刻（オプション）
     * @param success 成功フラグ
     * @param failMessage 失敗メッセージ（失敗時のみ）
     * @param failCode 失敗コード（失敗時のみ）
     */
    private void updatePaymentMeta(String orderNo,
                                   String paymentStatus,
                                   String eventId,
                                   LocalDateTime eventTime,
                                   boolean success,
                                   String failMessage,
                                   String failCode) {
        LocalDateTime now = LocalDateTime.now();
        // Ensure failMessage fits DB column (<=255). Truncate if necessary to avoid DataTruncation.
        if (failMessage != null && failMessage.length() > 255) {
            int originalLen = failMessage.length();
            log.warn("[PaymentService] updatePaymentMeta failMessage too long ({} chars), truncating to 255 for orderNo={}", originalLen, orderNo);
            failMessage = failMessage.substring(0, 255);
        }
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

    /**
     * 複数の文字列値から最初の非空白値を返します。
     * 
     * <p>使用例: 失敗コードの優先順位付け
     * - リクエストのコードが優先
     * - なければステータスをフォールバック
     * 
     * @param values チェック対象の文字列配列（null可）
     * @return 最初に見つかった非空白文字列、すべて空白の場合はnull
     */
    private static String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
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
