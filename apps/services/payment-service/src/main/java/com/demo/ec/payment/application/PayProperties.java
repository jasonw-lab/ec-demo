package com.demo.ec.payment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paypay")
public class PayProperties {
    /** Enable calling PayPay API. If false, stubbed sandbox link is used. */
    private boolean enabled;
    /** PayPay API base URL (e.g., https://api.paypay.ne.jp for prod, https://stg-api.paypay.ne.jp for sandbox). */
    private String baseUrl;
    /** Merchant ID to assume (optional). */
    private String merchantId;
    /** API Key (public key) */
    private String apiKey;
    /** API Secret */
    private String apiSecret;
    /** Redirect/Callback URL where PayPay will send the user after payment */
    private String callbackUrl;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
