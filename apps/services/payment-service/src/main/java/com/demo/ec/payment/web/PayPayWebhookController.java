package com.demo.ec.payment.web;

import com.demo.ec.payment.gateway.client.OrderServiceClient;
import com.demo.ec.payment.gateway.client.dto.PaymentStatusUpdateRequest;
import com.demo.ec.payment.gateway.messaging.PaymentEventPublisher;
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
 *   <li>Kafka に PaymentSucceeded イベントを publish</li>
 * </ol>
 */
@RestController
public class PayPayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayPayWebhookController.class);

    private final OrderServiceClient orderServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;

    public PayPayWebhookController(
            OrderServiceClient orderServiceClient,
            PaymentEventPublisher paymentEventPublisher) {
        this.orderServiceClient = orderServiceClient;
        this.paymentEventPublisher = paymentEventPublisher;
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

            // ✅ 決済成功時: Kafka に PaymentSucceeded イベントを publish（order-service 通知の成否に関わらず）
            boolean isPaymentSuccess = "COMPLETED".equalsIgnoreCase(message.status()) || "SUCCESS".equalsIgnoreCase(message.status());
            if (isPaymentSuccess) {
                publishPaymentSucceededEvent(message);
            }

            // order-serviceに支払いステータス更新を通知
            PaymentStatusUpdateRequest request = buildPaymentStatusRequest(message);
            Optional<java.util.Map> updated = orderServiceClient.notifyPaymentStatus(message.orderId(), request);

            if (updated.isPresent()) {
                return handleSuccessfulUpdate(message);
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
     * @return HTTPレスポンス（200 OK）
     */
    private ResponseEntity<?> handleSuccessfulUpdate(WebhookMessage message) {
        log.info("[PayPayWebhook] Order updated successfully orderId={}, eventId={}",
                message.orderId(), message.eventId());
        return ResponseEntity.ok(Map.of("success", true, "orderId", message.orderId()));
    }

    /**
     * PaymentSucceeded イベントを Kafka へ publish します。
     * 
     * @param message Webhookメッセージ
     */
    private void publishPaymentSucceededEvent(WebhookMessage message) {
        try {
            // paymentId は merchantPaymentId を使用、なければ eventId を使用
            String paymentId = StringUtils.hasText(message.merchantPaymentId()) 
                    ? message.merchantPaymentId() 
                    : message.eventId();
            
            paymentEventPublisher.publishPaymentSucceeded(
                    message.orderId(),
                    paymentId,
                    "PayPay",
                    message.amount(),
                    message.currency()
            );
            log.info("[PayPayWebhook] Published PaymentSucceeded to Kafka orderId={} paymentId={}", 
                    message.orderId(), paymentId);
        } catch (Exception e) {
            // Kafka publish 失敗は警告ログのみ（Webhook 処理自体は成功とする）
            log.warn("[PayPayWebhook] Failed to publish PaymentSucceeded to Kafka orderId={}, error={}",
                    message.orderId(), e.getMessage());
        }
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

    private record WebhookMessage(String orderId, String merchantPaymentId, String status, String code, String message, String eventId, String eventTime, Double amount, String currency) {
        static WebhookMessage fromPayload(Map<String, Object> payload) {
            // metadata.orderId を優先的に使用（テスト環境で orderId を渡すため）
            String orderId = null;
            Object metadataNode = payload.get("metadata");
            if (metadataNode instanceof Map<?, ?> metadata) {
                orderId = stringValue(metadata.get("orderId"));
            }
            
            String merchantPaymentId = stringValue(payload.get("merchantPaymentId"));
            // metadata.orderId がない場合は merchantPaymentId を orderId として使用
            if (!StringUtils.hasText(orderId)) {
                orderId = merchantPaymentId;
            }
            
            String status = stringValue(payload.get("status"));
            String code = stringValue(payload.get("code"));
            String message = stringValue(payload.get("message"));
            String eventId = stringValue(payload.get("eventId"));
            String eventTime = stringValue(payload.get("eventTime"));
            
            // 金額情報を取得
            Double amount = null;
            String currency = "JPY";
            Object amountNode = payload.get("amount");
            if (amountNode instanceof Map<?, ?> amountMap) {
                Object amountValue = amountMap.get("amount");
                if (amountValue instanceof Number) {
                    amount = ((Number) amountValue).doubleValue();
                }
                Object currencyValue = amountMap.get("currency");
                if (currencyValue != null) {
                    currency = stringValue(currencyValue);
                }
            }

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

            return new WebhookMessage(orderId, merchantPaymentId, normalize(status), code, message, eventId, eventTime, amount, currency);
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
