package com.demo.ec.paypay;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class PayPayService {

    private final PayPayProperties properties;

    public PayPayService(PayPayProperties properties) {
        this.properties = properties;
    }

    /**
     * Create a payment session and return a URL that the frontend can redirect to.
     * If PayPay integration is disabled, returns a sandbox demo URL.
     */
    public String createPaymentUrl(String orderId, BigDecimal amount, Map<String, Object> metadata) {
        if (!properties.isEnabled()) {
            // Demo stub: show a sandbox-like URL with order reference.
            return "https://sandbox.paypay.ne.jp/checkout?orderId=" + orderId;
        }
        // Placeholder for real integration: You would call PayPay API here and return redirect url
        // To keep this repository runnable without secrets, we fallback to demo URL even if enabled but missing keys.
        if (isBlank(properties.getApiKey()) || isBlank(properties.getApiSecret())) {
            return "https://sandbox.paypay.ne.jp/checkout?orderId=" + orderId;
        }

        // In a real implementation, you would:
        // 1) Construct payload (order amount, currency JPY, orderId, redirect/callback URL, etc.)
        // 2) Sign the request using API key/secret (HMAC SHA256) per PayPay docs
        // 3) POST to PayPay endpoint (properties.getBaseUrl() + "/v2/payments") or appropriate route
        // 4) Parse response and extract redirect URL
        // For now, return the demo URL to keep flow working.
        return "https://sandbox.paypay.ne.jp/checkout?orderId=" + orderId + "&nonce=" + UUID.randomUUID();
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
