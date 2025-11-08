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
 *   <li>支払い失敗時の注文ステータス更新（FAILED）</li>
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

        // 注文の存在確認
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.error("[PaymentService] Order not found orderNo={}", orderNo);
            throw new IllegalArgumentException("Order not found for orderNo=" + orderNo);
        }

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
        Order updated = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        log.info("[PaymentService] Payment status update completed orderNo={}, finalStatus={}", 
                orderNo, updated.getStatus());
        return updated;
    }

    /**
     * 支払い成功ステータスを処理します。
     */
    private void handleSuccessStatus(String orderNo, 
                                    String normalized, 
                                    OrderStatus currentStatus,
                                    PaymentStatusUpdateRequest request,
                                    LocalDateTime eventTime) {
        if (!OrderStatus.PAID.equals(currentStatus)) {
            log.info("[PaymentService] Confirming payment success orderNo={}, status={}", orderNo, normalized);
            // 在庫を確定
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderNo));
            orderSagaActions.storageConfirm(orderNo, order.getProductId(), order.getCount());
            // 注文をPAIDに更新
            orderSagaActions.markPaid(orderNo);
            log.info("[PaymentService] Order marked as PAID orderNo={}", orderNo);
        } else {
            log.info("[PaymentService] Order already PAID, ignoring state mutation orderNo={}", orderNo);
        }
        updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
    }

    /**
     * 支払い失敗ステータスを処理します。
     */
    private void handleFailureStatus(String orderNo, 
                                    String normalized, 
                                    OrderStatus currentStatus,
                                    PaymentStatusUpdateRequest request,
                                    LocalDateTime eventTime) {
        if (OrderStatus.PAID.equals(currentStatus)) {
            log.warn("[PaymentService] Received failure status after PAID, ignoring orderNo={}, status={}", 
                    orderNo, normalized);
            updatePaymentMeta(orderNo, normalized, request.getEventId(), eventTime, true, null, null);
            return;
        }
        
        String failCode = firstNonBlank(request.getCode(), normalized);
        String failMessage = firstNonBlank(request.getMessage(), "PayPay status " + normalized);
        
        if (!OrderStatus.FAILED.equals(currentStatus)) {
            log.info("[PaymentService] Marking order as FAILED orderNo={}, status={}, code={}", 
                    orderNo, normalized, failCode);
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderNo));
            orderSagaActions.markFailed(orderNo, failCode, failMessage);
            // 在庫を補償
            orderSagaActions.storageCompensate(orderNo, order.getProductId(), order.getCount());
            log.info("[PaymentService] Order marked as FAILED and stock compensated orderNo={}", orderNo);
        } else {
            log.info("[PaymentService] Order already FAILED, ignoring repeated failure event orderNo={}", orderNo);
        }
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
