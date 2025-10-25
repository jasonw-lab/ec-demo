package com.demo.ec.controller;

import com.demo.ec.pay.PayProperties;
import com.demo.ec.pay.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
                    .body(response(false, "INVALID_AMOUNT", "amount must be numeric", orderNo));
        }

        if (!StringUtils.hasText(orderNo)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(response(false, "ORDER_NO_REQUIRED", "orderNo is required", null));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(response(false, "AMOUNT_REQUIRED", "amount must be positive", orderNo));
        }

        try {
            boolean success = executePayment(orderNo, amount);
            String code = success ? "OK" : "DECLINED";
            String msg = success ? null : "Mock gateway declined the payment";
            return ResponseEntity.ok(response(success, code, msg, orderNo));
        } catch (Exception e) {
            log.warn("PayPay pay failed orderNo={} reqId={} err={}", orderNo, requestId, e.toString());
            return ResponseEntity.ok(response(false, "EXCEPTION", e.getMessage(), orderNo));
        }
    }

    private boolean executePayment(String orderNo, BigDecimal amount) {
        if (!payProperties.isEnabled()) {
            // Demo logic: permit payments up to 100,000 JPY. Above that we pretend PayPay declined it.
            boolean ok = amount.compareTo(BigDecimal.valueOf(100_000L)) <= 0;
            log.info("Mock PayPay payment processed orderNo={} amount={} result={}", orderNo, amount, ok ? "SUCCESS" : "DECLINED");
            return ok;
        }
        // Real integration path: leverage existing PaymentService to initiate the request.
        Map<String, Object> metadata = Map.of(
                "source", "order-service",
                "orderNo", orderNo
        );
        String url = paymentService.createPaymentUrl(orderNo, amount, metadata);
        log.info("PayPay payment initiated orderNo={} amount={} url={}", orderNo, amount, url);
        return url != null;
    }

    private static Map<String, Object> response(boolean success, String code, String message, String orderNo) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("code", code);
        if (message != null) {
            resp.put("message", message);
        }
        if (StringUtils.hasText(orderNo)) {
            resp.put("orderNo", orderNo);
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
}
