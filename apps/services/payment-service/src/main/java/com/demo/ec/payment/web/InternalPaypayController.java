package com.demo.ec.payment.web;

import com.demo.ec.payment.application.PayProperties;
import com.demo.ec.payment.application.PaymentService;
import com.demo.ec.payment.application.PaymentSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal-only controller that exposes a lightweight payment API for the Saga orchestrator.
 * The goal is to keep the business flow inside order-service deterministic by delegating PayPay calls
 * (or mocks) to this backend service.
 */
@RestController
@RequestMapping("/internal/payment/paypay")
public class InternalPaypayController {

    private static final Logger log = LoggerFactory.getLogger(InternalPaypayController.class);

    private final PaymentService paymentService;
    private final PayProperties payProperties;
    private final Map<String, Instant> paymentExpiryMap = new ConcurrentHashMap<>();
    private final Set<String> timedOutPayments = ConcurrentHashMap.newKeySet();

    public InternalPaypayController(PaymentService paymentService, PayProperties payProperties) {
        this.paymentService = paymentService;
        this.payProperties = payProperties;
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> pay(@RequestBody Map<String, Object> body,
                                                   @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                                   @RequestHeader(value = "X-Order-No", required = false) String headerOrderNo) {
        String orderNo = firstNonBlank(toStringSafe(body.get("orderNo")), headerOrderNo);
        BigDecimal amount;
        try {
            amount = body.containsKey("amount")
                    ? new BigDecimal(String.valueOf(body.get("amount")))
                    : null;
        } catch (NumberFormatException ex) {
            log.warn("Invalid amount received for orderNo={} reqId={}: {}", orderNo, requestId, body.get("amount"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse(false, "INVALID_AMOUNT", "FAILED", "amount must be numeric", orderNo, null));
        }

        if (!StringUtils.hasText(orderNo)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse(false, "ORDER_NO_REQUIRED", "FAILED", "orderNo is required", null, null));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse(false, "AMOUNT_REQUIRED", "FAILED", "amount must be positive", orderNo, null));
        }

        if (!payProperties.isEnabled()) {
            log.warn("PayPay integration disabled. Rejecting payment creation orderNo={} reqId={}", orderNo, requestId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(buildResponse(false, "PAYPAY_DISABLED", "DISABLED", "PayPay integration is disabled", orderNo, null));
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "order-service");
        metadata.put("orderNo", orderNo);

        try {
            PaymentSession session = paymentService.createPaymentSession(orderNo, amount, metadata);
            recordPaymentExpiry(session);
            return ResponseEntity.ok(buildResponse(true, "PENDING", "CREATED", null, orderNo, session));
        } catch (IllegalStateException e) {
            log.warn("PayPay session creation rejected orderNo={} reqId={} err={}", orderNo, requestId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse(false, "INVALID_CONFIGURATION", "FAILED", e.getMessage(), orderNo, null));
        } catch (Exception e) {
            log.error("PayPay session creation failed orderNo={} reqId={} err={}", orderNo, requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildResponse(false, "EXCEPTION", "FAILED", e.getMessage(), orderNo, null));
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/status/{merchantPaymentId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String merchantPaymentId) {
        log.info("PayPay status inquiry merchantPaymentId={}", merchantPaymentId);
        Map<String, Object> base = new HashMap<>();
        base.put("orderNo", merchantPaymentId);
        base.put("merchantPaymentId", merchantPaymentId);

        if (isTimedOut(merchantPaymentId, base)) {
            return ResponseEntity.ok(base);
        }

        if (!payProperties.isEnabled()) {
            base.put("success", false);
            base.put("code", "PAYPAY_DISABLED");
            base.put("status", "DISABLED");
            base.put("message", "PayPay integration is disabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(base);
        }

        try {
            Map<String, Object> details = paymentService.getPaymentDetails(merchantPaymentId);
            if (details == null) {
                base.put("success", false);
                base.put("code", "NOT_FOUND");
                base.put("status", "UNKNOWN");
                return ResponseEntity.ok(base);
            }
            Object error = details.get("error");
            if (error instanceof Map<?, ?> errorMap) {
                String code = toStringSafe(((Map<?, ?>) error).get("code"));
                String message = toStringSafe(((Map<?, ?>) error).get("message"));
                base.put("success", false);
                base.put("code", code != null ? code : "PAYPAY_ERROR");
                base.put("status", "FAILED");
                if (StringUtils.hasText(message)) {
                    base.put("message", message);
                }
                return ResponseEntity.ok(base);
            }
            String status = resolveStatus(details);
            base.put("status", status != null ? status : "UNKNOWN");
            boolean success = status != null && (status.equalsIgnoreCase("COMPLETED") || status.equalsIgnoreCase("SUCCESS"));
            boolean failure = status != null && (status.equalsIgnoreCase("FAILED")
                    || status.equalsIgnoreCase("CANCELLED") || status.equalsIgnoreCase("CANCELED")
                    || status.equalsIgnoreCase("DECLINED") || status.equalsIgnoreCase("EXPIRED"));
            base.put("success", success);
            base.put("code", success ? "OK" : (failure ? status : "PENDING"));
            if (success || failure) {
                clearPaymentTracking(merchantPaymentId);
            }
            return ResponseEntity.ok(base);
        } catch (Exception e) {
            log.error("PayPay status inquiry failed merchantPaymentId={} err={}", merchantPaymentId, e.getMessage(), e);
            base.put("success", false);
            base.put("code", "SYSTEM_ERROR");
            base.put("status", "FAILED");
            base.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(base);
        }
    }

    /**
     * Detect expired payment sessions and mark them as timed out.
     *
     * <p>Default interval: 60000ms (paypay.timeout-check-interval-ms)
     */
    @Scheduled(fixedDelayString = "${paypay.timeout-check-interval-ms:60000}")
    public void enforcePaymentTimeouts() {
        if (paymentExpiryMap.isEmpty()) {
            log.debug("PayPay timeout check skipped (no tracked payments)");
            return;
        }
        Instant now = Instant.now();
        int timedOut = 0;
        for (Map.Entry<String, Instant> entry : paymentExpiryMap.entrySet()) {
            Instant expiresAt = entry.getValue();
            if (expiresAt != null && now.isAfter(expiresAt)) {
                markTimedOut(entry.getKey(), expiresAt);
                timedOut++;
            }
        }
        if (timedOut > 0) {
            log.info("PayPay timeout check completed: timedOut={}", timedOut);
        }
    }

    private Map<String, Object> buildResponse(boolean success,
                                              String code,
                                              String status,
                                              String message,
                                              String orderNo,
                                              PaymentSession session) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("code", code);
        if (StringUtils.hasText(status)) {
            resp.put("status", status);
        }
        if (StringUtils.hasText(message)) {
            resp.put("message", message);
        }
        if (StringUtils.hasText(orderNo)) {
            resp.put("orderNo", orderNo);
            resp.put("merchantPaymentId", orderNo);
        }
        if (session != null) {
            resp.put("merchantPaymentId", session.getMerchantPaymentId());
            if (StringUtils.hasText(session.getPaymentUrl())) {
                resp.put("paymentUrl", session.getPaymentUrl());
            }
            if (StringUtils.hasText(session.getDeeplink())) {
                resp.put("deeplink", session.getDeeplink());
            }
            if (session.getExpiresAt() != null) {
                resp.put("expiresAt", session.getExpiresAt().toString());
            }
        }
        return resp;
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

    private static String toStringSafe(Object value) {
        return value == null ? null : Objects.toString(value);
    }

    private void recordPaymentExpiry(PaymentSession session) {
        if (session == null) {
            return;
        }
        Instant expiresAt = session.getExpiresAt();
        if (expiresAt != null) {
            paymentExpiryMap.put(session.getMerchantPaymentId(), expiresAt);
            timedOutPayments.remove(session.getMerchantPaymentId());
        }
    }

    private boolean isTimedOut(String merchantPaymentId, Map<String, Object> base) {
        if (timedOutPayments.contains(merchantPaymentId)) {
            applyTimeoutResponse(base);
            return true;
        }
        Instant expiresAt = paymentExpiryMap.get(merchantPaymentId);
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            markTimedOut(merchantPaymentId, expiresAt);
            applyTimeoutResponse(base);
            return true;
        }
        return false;
    }

    private void markTimedOut(String merchantPaymentId, Instant expiresAt) {
        timedOutPayments.add(merchantPaymentId);
        paymentExpiryMap.remove(merchantPaymentId, expiresAt);
        log.info("PayPay payment timed out merchantPaymentId={} expiresAt={}", merchantPaymentId, expiresAt);
    }

    private static void applyTimeoutResponse(Map<String, Object> base) {
        base.put("success", false);
        base.put("code", "PAYPAY_TIMEOUT");
        base.put("status", "EXPIRED");
        base.put("message", "PayPay payment timed out");
    }

    private void clearPaymentTracking(String merchantPaymentId) {
        paymentExpiryMap.remove(merchantPaymentId);
        timedOutPayments.remove(merchantPaymentId);
    }

    @SuppressWarnings("unchecked")
    private static String resolveStatus(Map<String, Object> details) {
        Object data = details.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object status = ((Map<String, Object>) dataMap).get("status");
            if (status == null) {
                status = ((Map<String, Object>) dataMap).get("paymentStatus");
            }
            if (status != null) {
                return String.valueOf(status);
            }
        }
        Object status = details.get("status");
        return status == null ? null : String.valueOf(status);
    }
}
