package com.demo.ec.payment.application;

import java.time.Instant;

/**
 * Represents a PayPay payment session returned when creating a dynamic QR code.
 */
public class PaymentSession {
    private final String merchantPaymentId;
    private final String paymentUrl;
    private final String deeplink;
    private final Instant expiresAt;

    public PaymentSession(String merchantPaymentId, String paymentUrl, String deeplink, Instant expiresAt) {
        this.merchantPaymentId = merchantPaymentId;
        this.paymentUrl = paymentUrl;
        this.deeplink = deeplink;
        this.expiresAt = expiresAt;
    }

    public String getMerchantPaymentId() {
        return merchantPaymentId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public String getDeeplink() {
        return deeplink;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
