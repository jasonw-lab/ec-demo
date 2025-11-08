package com.demo.ec.controller;

import com.demo.ec.client.OrderServiceClient;
import com.demo.ec.client.dto.OrderSummary;
import com.demo.ec.client.dto.PaymentStatusUpdateRequest;
import com.demo.ec.ws.OrderStatusBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * PayPay Webhook受信コントローラー
 * 
 * <p>PayPayから送信されるWebhookイベントを受信し、order-serviceに転送します。
 * Webhookはポーリングよりも優先され、リアルタイムに支払い完了を検知できます。
 * 
 * <p>エンドポイント：
 * <ul>
 *   <li>/paypay/callback - Cloudflare Tunnel経由でPayPayから呼ばれる</li>
 *   <li>/api/paypay/webhook - 内部API用（テスト等）</li>
 * </ul>
 * 
 * <p>処理フロー：
 * <ol>
 *   <li>Webhookペイロードを受信</li>
 *   <li>order-serviceに支払いステータス更新を通知</li>
 *   <li>WebSocketでフロントエンドに通知</li>
 * </ol>
 */
@RestController
public class PayPayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayPayWebhookController.class);

    private final OrderServiceClient orderServiceClient;
    private final OrderStatusBroadcaster broadcaster;

    public PayPayWebhookController(OrderServiceClient orderServiceClient, OrderStatusBroadcaster broadcaster) {
        this.orderServiceClient = orderServiceClient;
        this.broadcaster = broadcaster;
    }

    /**
     * Cloudflare Tunnel経由でPayPayから呼ばれるコールバックエンドポイント
     * 
     * <p>URL: https://skip-denied-oils-reflected.trycloudflare.com/paypay/callback
     * 
     * @param payload Webhookペイロード
     * @return HTTP 200 OK（成功時）
     */
    @PostMapping("/paypay/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, Object> payload) {
        log.info("[PayPayWebhook] Received callback at /paypay/callback payloadKeys={}", payload.keySet());
        return processWebhook(payload);
    }

    /**
     * 既存のwebhookエンドポイント（内部API用）
     * 
     * <p>URL: /api/paypay/webhook
     * テストやデバッグ用途で使用します。
     * 
     * @param payload Webhookペイロード
     * @return HTTP 200 OK（成功時）
     */
    @PostMapping("/api/paypay/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        log.info("[PayPayWebhook] Received webhook at /api/paypay/webhook payloadKeys={}", payload.keySet());
        return processWebhook(payload);
    }

    /**
     * Webhookペイロードを処理し、order-serviceに通知します。
     * 
     * <p>Webhookはポーリングよりも優先されるため、即座に処理されます。
     * 
     * @param payload Webhookペイロード
     * @return HTTPレスポンス
     */
    private ResponseEntity<?> processWebhook(Map<String, Object> payload) {
        try {
            // ペイロードから注文情報を抽出
            WebhookMessage message = WebhookMessage.fromPayload(payload);
            
            // 必須項目の検証
            if (!StringUtils.hasText(message.orderId()) || !StringUtils.hasText(message.status())) {
                log.warn("[PayPayWebhook] Missing required fields orderId={}, status={}, payload={}", 
                        message.orderId(), message.status(), payload);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "orderId/status required"));
            }

            log.info("[PayPayWebhook] Processing payment status update orderId={}, status={}, eventId={}", 
                    message.orderId(), message.status(), message.eventId());

            // order-serviceに支払いステータス更新を通知
            PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
            request.setStatus(message.status());
            request.setCode(message.code());
            request.setMessage(message.message());
            request.setEventId(message.eventId());
            request.setEventTime(message.eventTime());

            Optional<OrderSummary> updated = orderServiceClient.notifyPaymentStatus(message.orderId(), request);
            
            if (updated.isPresent()) {
                OrderSummary summary = updated.get();
                log.info("[PayPayWebhook] Order updated successfully orderId={}, newStatus={}, eventId={}", 
                        message.orderId(), summary.getStatus(), message.eventId());
                
                // WebSocketでフロントエンドに通知
                broadcaster.broadcast(summary);
                log.info("[PayPayWebhook] WebSocket broadcast sent orderId={}, eventId={}", 
                        message.orderId(), message.eventId());
                
                return ResponseEntity.ok(Map.of("success", true, "orderId", message.orderId()));
            } else {
                log.warn("[PayPayWebhook] Order update failed orderId={}, eventId={} (order service returned empty)", 
                        message.orderId(), message.eventId());
                return ResponseEntity.ok(Map.of("success", false, "message", "orderNotUpdated", "orderId", message.orderId()));
            }
        } catch (Exception e) {
            log.error("[PayPayWebhook] Webhook processing failed payload={}, error={}", payload, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }

    private record WebhookMessage(String orderId, String status, String code, String message, String eventId, String eventTime) {
        static WebhookMessage fromPayload(Map<String, Object> payload) {
            String orderId = stringValue(payload.get("merchantPaymentId"));
            String status = stringValue(payload.get("status"));
            String code = stringValue(payload.get("code"));
            String message = stringValue(payload.get("message"));
            String eventId = stringValue(payload.get("eventId"));
            String eventTime = stringValue(payload.get("eventTime"));

            Object dataNode = payload.get("data");
            if (dataNode instanceof Map<?, ?> data) {
                orderId = firstNonBlank(orderId, stringValue(data.get("merchantPaymentId")), stringValue(data.get("orderId")));
                status = firstNonBlank(status, stringValue(data.get("status")), stringValue(data.get("paymentStatus")));
                code = firstNonBlank(code, stringValue(data.get("code")));
                message = firstNonBlank(message, stringValue(data.get("message")));
                Object event = data.get("eventTime");
                if (event != null && !StringUtils.hasText(eventTime)) {
                    eventTime = String.valueOf(event);
                }
            }

            Object resultInfo = payload.get("resultInfo");
            if (resultInfo instanceof Map<?, ?> info) {
                code = firstNonBlank(code, stringValue(info.get("code")));
                message = firstNonBlank(message, stringValue(info.get("message")));
            }

            if (!StringUtils.hasText(eventTime) && payload.get("eventDate") != null) {
                eventTime = String.valueOf(payload.get("eventDate"));
            }
            if (!StringUtils.hasText(eventTime) && payload.get("timestamp") instanceof Number number) {
                eventTime = Instant.ofEpochMilli(number.longValue()).toString();
            }

            return new WebhookMessage(orderId, normalize(status), code, message, eventId, eventTime);
        }

        private static String normalize(String status) {
            return status == null ? null : status.trim().toUpperCase();
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
}
