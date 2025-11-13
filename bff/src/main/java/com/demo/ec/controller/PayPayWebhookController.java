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
     * <p>重要な設計判断：
     * - Webhookはポーリングよりも優先されるため、即座に処理
     * - 例外処理を適切に行い、Webhook送信元（PayPay）には常に200を返す
     * - バリデーション失敗時は400を返すが、処理エラー時は200を返す（再送防止）
     * 
     * @param payload Webhookペイロード（非null保証済み）
     * @return HTTPレスポンス（常に200または400）
     */
    private ResponseEntity<?> processWebhook(Map<String, Object> payload) {
        // null安全性チェック
        if (payload == null || payload.isEmpty()) {
            log.warn("[PayPayWebhook] Empty or null payload received");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Payload is required"));
        }

        try {
            // ペイロードから注文情報を抽出
            // 注意: ペイロードの構造が想定と異なる場合、例外が発生する可能性がある
            WebhookMessage message = WebhookMessage.fromPayload(payload);
            
            // 必須項目の検証（ビジネスロジックの前提条件）
            ValidationResult validation = validateWebhookMessage(message);
            if (!validation.isValid()) {
                log.warn("[PayPayWebhook] Validation failed: {}, payloadKeys={}", 
                        validation.errorMessage(), payload.keySet());
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", validation.errorMessage()));
            }

            log.info("[PayPayWebhook] Processing payment status update orderId={}, status={}, eventId={}", 
                    message.orderId(), message.status(), message.eventId());

            // order-serviceに支払いステータス更新を通知
            PaymentStatusUpdateRequest request = buildPaymentStatusRequest(message);
            Optional<OrderSummary> updated = orderServiceClient.notifyPaymentStatus(message.orderId(), request);
            
            if (updated.isPresent()) {
                return handleSuccessfulUpdate(message, updated.get());
            } else {
                // 注文が見つからない、または更新に失敗した場合
                // ■ Webhookの再送を防ぐために200を返す
                // PayPayが再送すると同じイベントが何度も処理される恐れがある
                log.warn("[PayPayWebhook] Order update returned empty orderId={}, eventId={}", 
                        message.orderId(), message.eventId());
                return ResponseEntity.ok(Map.of(
                        "success", false, 
                        "message", "orderNotUpdated", 
                        "orderId", message.orderId()));
            }
        } catch (IllegalArgumentException e) {
            // バリデーションエラー（ペイロードの構造が不正など）
            log.warn("[PayPayWebhook] Invalid payload structure: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid payload: " + e.getMessage()));
        } catch (Exception e) {
            // 予期しないエラー
            // ■ Webhookの再送を防ぐために500ではなく200を返す
            // 一時的な障害で再送されると同じエラーが繰り返される恐れがある
            log.error("[PayPayWebhook] Unexpected error processing webhook payload={}, error={}", 
                    payload, e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false, 
                    "message", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Webhookメッセージのバリデーションを行います。
     * 
     * @param message Webhookメッセージ（非null保証済み）
     * @return バリデーション結果
     */
    private ValidationResult validateWebhookMessage(WebhookMessage message) {
        if (!StringUtils.hasText(message.orderId())) {
            return ValidationResult.invalid("orderId is required");
        }
        if (!StringUtils.hasText(message.status())) {
            return ValidationResult.invalid("status is required");
        }
        return ValidationResult.valid();
    }

    /**
     * PaymentStatusUpdateRequestを構築します。
     * 
     * @param message Webhookメッセージ（非null保証済み）
     * @return 構築されたリクエスト（非null保証）
     */
    private PaymentStatusUpdateRequest buildPaymentStatusRequest(WebhookMessage message) {
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
        request.setStatus(message.status());
        request.setCode(message.code());
        request.setMessage(message.message());
        request.setEventId(message.eventId());
        request.setEventTime(message.eventTime());
        return request;
    }

    /**
     * 注文更新成功時の処理を行います。
     * 
     * @param message Webhookメッセージ（非null保証済み）
     * @param summary 更新された注文サマリー（非null保証済み）
     * @return HTTPレスポンス（200 OK）
     */
    private ResponseEntity<?> handleSuccessfulUpdate(WebhookMessage message, OrderSummary summary) {
        log.info("[PayPayWebhook] Order updated successfully orderId={}, newStatus={}, eventId={}", 
                message.orderId(), summary.getStatus(), message.eventId());
        
        // WebSocketでフロントエンドに通知
        // ■ ブロッキング処理だがWebhook処理の一部として実行する
        // ユーザーに即座に通知する必要がある
        try {
            broadcaster.broadcast(summary);
            log.info("[PayPayWebhook] WebSocket broadcast sent orderId={}, eventId={}", 
                    message.orderId(), message.eventId());
        } catch (Exception e) {
            // WebSocket通知の失敗はログに記録するがWebhook処理は成功扱いとする
            // ■ WebSocket通知は補助的な機能であり、注文更新は完了している
            log.warn("[PayPayWebhook] WebSocket broadcast failed orderId={}, error={}", 
                    message.orderId(), e.getMessage(), e);
        }
        
        return ResponseEntity.ok(Map.of("success", true, "orderId", message.orderId()));
    }

    /**
     * バリデーション結果を保持するレコード。
     * 
     * @param isValid バリデーション成功フラグ
     * @param errorMessage エラーメッセージ（無効な場合のみ）
     */
    private record ValidationResult(boolean isValid, String errorMessage) {
        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
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
