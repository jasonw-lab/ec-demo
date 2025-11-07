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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Placeholder webhook endpoint for PayPay callbacks.
 * Real-world deployments should verify signatures and forward the event to order-service.
 */
@RestController
@RequestMapping("/api/paypay")
public class PayPayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayPayWebhookController.class);

    private final OrderServiceClient orderServiceClient;
    private final OrderStatusBroadcaster broadcaster;

    public PayPayWebhookController(OrderServiceClient orderServiceClient, OrderStatusBroadcaster broadcaster) {
        this.orderServiceClient = orderServiceClient;
        this.broadcaster = broadcaster;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        log.info("Received PayPay webhook payload={}", payload);
        WebhookMessage message = WebhookMessage.fromPayload(payload);
        if (!StringUtils.hasText(message.orderId()) || !StringUtils.hasText(message.status())) {
            log.warn("Webhook missing orderId/status payload={}", payload);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "orderId/status required"));
        }

        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
        request.setStatus(message.status());
        request.setCode(message.code());
        request.setMessage(message.message());
        request.setEventId(message.eventId());
        request.setEventTime(message.eventTime());

        Optional<OrderSummary> updated = orderServiceClient.notifyPaymentStatus(message.orderId(), request);
        if (updated.isPresent()) {
            broadcaster.broadcast(updated.get());
            return ResponseEntity.ok(Map.of("success", true));
        }

        log.warn("Webhook could not update order orderId={} (order service returned empty)", message.orderId());
        return ResponseEntity.ok(Map.of("success", false, "message", "orderNotUpdated"));
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
