package com.demo.ec.order.application;

import com.demo.ec.order.web.dto.PaymentStatusUpdateRequest;
import com.demo.ec.order.domain.Order;
import com.demo.ec.order.gateway.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * PayPay Webhook処理サービス
 * 
 * <p>PayPayから送信されるWebhookイベントを処理し、注文ステータスを更新します。
 * Webhookはポーリングよりも優先され、リアルタイムに支払い完了を検知できます。
 * 
 * <p>処理内容：
 * - Webhookペイロードから注文情報を抽出
 * - 注文ステータスを更新（PAID/CANCELLED）
 * - 重複イベントの検知と防止
 */
@Service
public class PaymentWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookHandler.class);

    private final OrderMapper orderMapper;
    private final OrderPaymentService orderPaymentService;

    public PaymentWebhookHandler(OrderMapper orderMapper, OrderPaymentService orderPaymentService) {
        this.orderMapper = orderMapper;
        this.orderPaymentService = orderPaymentService;
    }

    /**
     * Webhookイベントを処理し、注文ステータスを更新します。
     * 
     * <p>Webhookはポーリングよりも優先されるため、即座に処理されます。
     * 重複イベントは自動的に検知され、無視されます。
     * 
     * @param orderNo 注文番号（merchantPaymentId）
     * @param status 支払いステータス（COMPLETED, FAILED等）
     * @param code エラーコード（オプション）
     * @param message エラーメッセージ（オプション）
     * @param eventId WebhookイベントID（重複検知用）
     * @param eventTime イベント発生時刻
     * @return 更新された注文、または注文が見つからない場合null
     */
    @Transactional
    public Order handleWebhook(String orderNo, 
                              String status, 
                              String code, 
                              String message, 
                              String eventId, 
                              String eventTime) {
        log.info("[PaymentWebhook] Processing webhook event orderNo={}, status={}, eventId={}", 
                orderNo, status, eventId);

        // 注文の存在確認
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        
        if (order == null) {
            log.warn("[PaymentWebhook] Order not found orderNo={}, eventId={}", orderNo, eventId);
            return null;
        }

        log.info("[PaymentWebhook] Order found orderNo={}, currentStatus={}, eventId={}", 
                orderNo, order.getStatus(), eventId);

        // 重複イベントのチェック
        if (StringUtils.hasText(eventId) && eventId.equals(order.getPaymentLastEventId())) {
            log.info("[PaymentWebhook] Duplicate event ignored orderNo={}, eventId={}, lastEventId={}", 
                    orderNo, eventId, order.getPaymentLastEventId());
            return order;
        }

        // ステータス更新リクエストを作成
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
        request.setStatus(status);
        request.setCode(code);
        request.setMessage(message);
        request.setEventId(eventId);
        request.setEventTime(eventTime);

        try {
            // 注文ステータスを更新
            Order updated = orderPaymentService.handlePaymentStatus(orderNo, request);
            
            log.info("[PaymentWebhook] Order status updated successfully orderNo={}, " +
                    "oldStatus={}, newStatus={}, eventId={}", 
                    orderNo, order.getStatus(), updated.getStatus(), eventId);
            
            return updated;
        } catch (Exception e) {
            log.error("[PaymentWebhook] Failed to update order status orderNo={}, eventId={}, error={}", 
                     orderNo, eventId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Webhookペイロードから注文情報を抽出して処理します。
     * 
     * @param payload Webhookペイロード
     * @return 更新された注文、または処理できない場合null
     */
    public Order handleWebhookPayload(java.util.Map<String, Object> payload) {
        log.info("[PaymentWebhook] Received webhook payload keys={}", payload.keySet());
        
        // ペイロードから情報を抽出
        String orderId = extractOrderId(payload);
        String status = extractStatus(payload);
        String code = extractCode(payload);
        String message = extractMessage(payload);
        String eventId = extractEventId(payload);
        String eventTime = extractEventTime(payload);

        // 必須項目の検証
        if (!StringUtils.hasText(orderId) || !StringUtils.hasText(status)) {
            log.warn("[PaymentWebhook] Missing required fields orderId={}, status={}, payload={}", 
                    orderId, status, payload);
            return null;
        }

        // 正規化
        String normalizedStatus = status.trim().toUpperCase();
        
        log.info("[PaymentWebhook] Extracted webhook data orderId={}, status={} (normalized={}), " +
                "code={}, eventId={}", orderId, status, normalizedStatus, code, eventId);

        return handleWebhook(orderId, normalizedStatus, code, message, eventId, eventTime);
    }

    /**
     * ペイロードから注文ID（merchantPaymentId）を抽出します。
     */
    private String extractOrderId(java.util.Map<String, Object> payload) {
        String orderId = stringValue(payload.get("merchantPaymentId"));
        
        Object dataNode = payload.get("data");
        if (dataNode instanceof java.util.Map<?, ?> data) {
            orderId = firstNonBlank(orderId, 
                    stringValue(data.get("merchantPaymentId")), 
                    stringValue(data.get("orderId")));
        }
        
        return orderId;
    }

    /**
     * ペイロードからステータスを抽出します。
     */
    private String extractStatus(java.util.Map<String, Object> payload) {
        String status = stringValue(payload.get("status"));
        
        Object dataNode = payload.get("data");
        if (dataNode instanceof java.util.Map<?, ?> data) {
            status = firstNonBlank(status, 
                    stringValue(data.get("status")), 
                    stringValue(data.get("paymentStatus")));
        }
        
        return status;
    }

    /**
     * ペイロードからエラーコードを抽出します。
     */
    private String extractCode(java.util.Map<String, Object> payload) {
        String code = stringValue(payload.get("code"));
        
        Object dataNode = payload.get("data");
        if (dataNode instanceof java.util.Map<?, ?> data) {
            code = firstNonBlank(code, stringValue(data.get("code")));
        }
        
        Object resultInfo = payload.get("resultInfo");
        if (resultInfo instanceof java.util.Map<?, ?> info) {
            code = firstNonBlank(code, stringValue(info.get("code")));
        }
        
        return code;
    }

    /**
     * ペイロードからメッセージを抽出します。
     */
    private String extractMessage(java.util.Map<String, Object> payload) {
        String message = stringValue(payload.get("message"));
        
        Object dataNode = payload.get("data");
        if (dataNode instanceof java.util.Map<?, ?> data) {
            message = firstNonBlank(message, stringValue(data.get("message")));
        }
        
        Object resultInfo = payload.get("resultInfo");
        if (resultInfo instanceof java.util.Map<?, ?> info) {
            message = firstNonBlank(message, stringValue(info.get("message")));
        }
        
        return message;
    }

    /**
     * ペイロードからイベントIDを抽出します。
     */
    private String extractEventId(java.util.Map<String, Object> payload) {
        return stringValue(payload.get("eventId"));
    }

    /**
     * ペイロードからイベント時刻を抽出します。
     */
    private String extractEventTime(java.util.Map<String, Object> payload) {
        String eventTime = stringValue(payload.get("eventTime"));
        
        Object dataNode = payload.get("data");
        if (dataNode instanceof java.util.Map<?, ?> data) {
            Object event = data.get("eventTime");
            if (event != null && !StringUtils.hasText(eventTime)) {
                eventTime = String.valueOf(event);
            }
        }
        
        if (!StringUtils.hasText(eventTime) && payload.get("eventDate") != null) {
            eventTime = String.valueOf(payload.get("eventDate"));
        }
        
        if (!StringUtils.hasText(eventTime) && payload.get("timestamp") instanceof Number number) {
            eventTime = java.time.Instant.ofEpochMilli(number.longValue()).toString();
        }
        
        return eventTime;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
