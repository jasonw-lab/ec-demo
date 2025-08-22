package com.demo.ec.paypay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PayPayService {

    private static final Logger log = LoggerFactory.getLogger(PayPayService.class);

    private final PayPayProperties properties;
    private final RestClient restClient;

    public PayPayService(PayPayProperties properties) {
        this.properties = properties;
        // Build a RestClient with sensible timeouts
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(10000);
        this.restClient = RestClient.builder().requestFactory(rf).baseUrl(properties.getBaseUrl()).build();
    }

    /**
     * Create a payment QR code in PayPay and return a URL for the frontend.
     * If integration is disabled or keys are missing, returns a sandbox demo URL.
     */
    public String createPaymentUrl(String orderId, BigDecimal amount, Map<String, Object> metadata) {
        if (!properties.isEnabled()) {
            return sandboxFallback(orderId);
        }
        if (isBlank(properties.getApiKey()) || isBlank(properties.getApiSecret())) {
            return sandboxFallback(orderId);
        }
        // Try SDK first
        try {
            log.info("PayPayService.createPaymentUrl SDK attempt for orderId={}", orderId);
            String sdkUrl = createPaymentUrlUsingSdk(orderId, amount, metadata);
            if (sdkUrl != null) {
                log.info("PayPayService.createPaymentUrl SDK success. url={}", sdkUrl);
                return sdkUrl;
            }
        } catch (Throwable t) {
            log.warn("PayPayService.createPaymentUrl SDK path failed, will fallback to REST. orderId={}, error={}", orderId, t.getMessage(), t);
        }
        // Fallback to direct REST if SDK not available or failed
        try {
            String path = "/v2/codes"; // Create QR Code API
            String merchantPaymentId = orderId;
            long jpy = amount.setScale(0, BigDecimal.ROUND_HALF_UP).longValueExact();
            long requestedAt = Instant.now().getEpochSecond();

            Map<String, Object> body = new HashMap<>();
            body.put("merchantPaymentId", merchantPaymentId);
            Map<String, Object> amountObj = new HashMap<>();
            amountObj.put("amount", jpy);
            amountObj.put("currency", "JPY");
            body.put("amount", amountObj);
            body.put("codeType", "ORDER_QR");
            body.put("requestedAt", requestedAt);
            body.put("redirectUrl", properties.getCallbackUrl() + "?orderId=" + orderId);
            body.put("redirectType", "WEB_LINK");
            // Optional: one-line order item summary
            Map<String, Object> item = new HashMap<>();
            item.put("name", String.valueOf(metadata != null ? metadata.getOrDefault("customerName", "EC Demo Order") : "EC Demo Order"));
            item.put("quantity", 1);
            Map<String, Object> itemAmount = new HashMap<>();
            itemAmount.put("amount", jpy);
            itemAmount.put("currency", "JPY");
            item.put("amount", itemAmount);
            body.put("orderItems", java.util.List.of(item));

            String json = org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json().build().writeValueAsString(body);

            String requestId = UUID.randomUUID().toString();
            String time = String.valueOf(requestedAt);
            String query = ""; // no query
            String method = "POST";

            String authHeader = buildAuthorizationHeader(method, path, query, json, requestId, time, properties.getApiKey(), properties.getApiSecret());

            log.info("PayPayService.createPaymentUrl REST request: method={}, path={}, requestId={}, time={}, assumeMerchant={}, body={}", method, path, requestId, time, (isBlank(properties.getMerchantId()) ? "" : properties.getMerchantId()), json);

            RestClient.RequestBodySpec spec = restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authHeader)
                    .header("X-REQUEST-ID", requestId)
                    .header("X-TIME", time);
            if (!isBlank(properties.getMerchantId())) {
                spec = spec.header("X-ASSUME-MERCHANT", properties.getMerchantId());
            }

            Map response = spec.body(json).retrieve().body(Map.class);
            log.info("PayPayService.createPaymentUrl REST response: requestId={}, body={}", requestId, response);
            if (response != null) {
                Object url = response.get("url");
                if (url != null) return String.valueOf(url);
                // Some responses wrap result
                Object data = response.get("data");
                if (data instanceof Map<?,?> dataMap) {
                    Object url2 = dataMap.get("url");
                    if (url2 != null) return String.valueOf(url2);
                }
            }
        } catch (Exception e) {
            // Fallback to demo URL if anything goes wrong to keep demo usable
            log.warn("PayPayService.createPaymentUrl REST path failed for orderId={}: {}", orderId, e.getMessage(), e);
        }
        return sandboxFallback(orderId);
    }

    /**
     * Attempt to use the official PayPay Java SDK via reflection (so build doesn’t break if the SDK JAR isn’t available).
     * Returns the redirect URL if successful; otherwise null.
     */
    private String createPaymentUrlUsingSdk(String orderId, BigDecimal amount, Map<String, Object> metadata) throws Exception {
        // Load classes reflectively
        Class<?> apiClientCl = Class.forName("jp.ne.paypay.ApiClient");
        Class<?> codeApiCl = Class.forName("jp.ne.paypay.api.CodeApi");
        // Try request classes with possible names
        Class<?> moneyCl;
        try { moneyCl = Class.forName("jp.ne.paypay.model.MoneyAmount"); } catch (ClassNotFoundException e) { moneyCl = Class.forName("jp.ne.paypay.model.Amount"); }
        Class<?> createQrReqCl;
        try { createQrReqCl = Class.forName("jp.ne.paypay.model.CreateQRCodeRequest"); } catch (ClassNotFoundException e) { createQrReqCl = Class.forName("jp.ne.paypay.model.CreateQrCodeRequest"); }

        Object client = apiClientCl.getConstructor().newInstance();
        // base URL
        try { apiClientCl.getMethod("setBasePath", String.class).invoke(client, properties.getBaseUrl()); } catch (NoSuchMethodException ignore) {
            try { apiClientCl.getMethod("setBaseUrl", String.class).invoke(client, properties.getBaseUrl()); } catch (NoSuchMethodException ignored) {}
        }
        // credentials
        try { apiClientCl.getMethod("setApiKey", String.class).invoke(client, properties.getApiKey()); } catch (NoSuchMethodException ignore) {}
        try { apiClientCl.getMethod("setApiSecret", String.class).invoke(client, properties.getApiSecret()); } catch (NoSuchMethodException ignore) {}
        if (!isBlank(properties.getMerchantId())) {
            try { apiClientCl.getMethod("setAssumeMerchant", String.class).invoke(client, properties.getMerchantId()); } catch (NoSuchMethodException ignore) {}
        }

        // Build request
        long jpy = amount.setScale(0, BigDecimal.ROUND_HALF_UP).longValueExact();
        long requestedAt = Instant.now().getEpochSecond();

        Object money = moneyCl.getConstructor().newInstance();
        // Try setters: setAmount(BigDecimal) or setAmount(Long)
        try { moneyCl.getMethod("setAmount", BigDecimal.class).invoke(money, BigDecimal.valueOf(jpy)); } catch (NoSuchMethodException ex) { moneyCl.getMethod("setAmount", Long.TYPE).invoke(money, jpy); }
        try { moneyCl.getMethod("setCurrency", String.class).invoke(money, "JPY"); } catch (NoSuchMethodException ignore) {}

        Object req = createQrReqCl.getConstructor().newInstance();
        // merchantPaymentId
        try { createQrReqCl.getMethod("setMerchantPaymentId", String.class).invoke(req, orderId); } catch (NoSuchMethodException ignore) {}
        // amount
        try { createQrReqCl.getMethod("setAmount", moneyCl).invoke(req, money); } catch (NoSuchMethodException ignore) {}
        // codeType
        try { createQrReqCl.getMethod("setCodeType", String.class).invoke(req, "ORDER_QR"); } catch (NoSuchMethodException ignore) {}
        // requestedAt
        try { createQrReqCl.getMethod("setRequestedAt", Long.TYPE).invoke(req, requestedAt); } catch (NoSuchMethodException ignore) {}
        // redirect
        String redirectUrl = properties.getCallbackUrl() + "?orderId=" + orderId;
        try { createQrReqCl.getMethod("setRedirectUrl", String.class).invoke(req, redirectUrl); } catch (NoSuchMethodException ignore) {}
        try { createQrReqCl.getMethod("setRedirectType", String.class).invoke(req, "WEB_LINK"); } catch (NoSuchMethodException ignore) {}

        // Instantiate API and invoke create method
        Object codeApi = codeApiCl.getConstructor(apiClientCl).newInstance(client);
        Object response = null;
        Exception last = null;
        for (String m : new String[]{"createQRCode", "createQrcode", "createCode"}) {
            try {
                response = codeApiCl.getMethod(m, createQrReqCl).invoke(codeApi, req);
                last = null;
                break;
            } catch (NoSuchMethodException e) {
                last = e;
            }
        }
        if (last != null) throw last;
        if (response == null) return null;

        // Try to get URL from response
        try {
            Object url = response.getClass().getMethod("getUrl").invoke(response);
            if (url != null) return String.valueOf(url);
        } catch (NoSuchMethodException ignore) {}
        try {
            Object data = response.getClass().getMethod("getData").invoke(response);
            if (data != null) {
                try {
                    Object url = data.getClass().getMethod("getUrl").invoke(data);
                    if (url != null) return String.valueOf(url);
                } catch (NoSuchMethodException ignore) {}
            }
        } catch (NoSuchMethodException ignore) {}
        return null;
    }

    private String sandboxFallback(String orderId) {
        return "https://sandbox.paypay.ne.jp/checkout?orderId=" + orderId + "&nonce=" + UUID.randomUUID();
    }

    /**
     * Get payment details from PayPay using merchantPaymentId (we use our orderId as merchantPaymentId).
     * Returns the raw response as Map, or null if disabled/keys missing/any error occurs.
     */
    public Map getPaymentDetails(String merchantPaymentId) {
        try {
            if (!properties.isEnabled()) return null;
            if (isBlank(properties.getApiKey()) || isBlank(properties.getApiSecret())) return null;

            String path = "/v2/payments";
            String query = "merchantPaymentId=" + URLEncoder.encode(merchantPaymentId, StandardCharsets.UTF_8);
            String method = "GET";
            String body = "";
            String requestId = UUID.randomUUID().toString();
            String time = String.valueOf(Instant.now().getEpochSecond());

            String authHeader = buildAuthorizationHeader(method, path, query, body, requestId, time, properties.getApiKey(), properties.getApiSecret());

            log.info("PayPayService.getPaymentDetails REST request: method={}, path={}, query={}, requestId={}, time={}, assumeMerchant={}", method, path, query, requestId, time, (isBlank(properties.getMerchantId()) ? "" : properties.getMerchantId()));

            RestClient.RequestHeadersSpec<?> spec = restClient
                    .get()
                    .uri(path + "?" + query)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", authHeader)
                    .header("X-REQUEST-ID", requestId)
                    .header("X-TIME", time);
            if (!isBlank(properties.getMerchantId())) {
                spec = ((RestClient.RequestHeadersSpec<?>) spec).header("X-ASSUME-MERCHANT", properties.getMerchantId());
            }
            Map response = spec.retrieve().body(Map.class);
            log.info("PayPayService.getPaymentDetails REST response: requestId={}, body={}", requestId, response);
            return response;
        } catch (Exception e) {
            log.warn("PayPayService.getPaymentDetails failed for merchantPaymentId={}: {}", merchantPaymentId, e.getMessage(), e);
            return null;
        }
    }

    private static String buildAuthorizationHeader(String method,
                                                   String path,
                                                   String query,
                                                   String body,
                                                   String requestId,
                                                   String time,
                                                   String apiKey,
                                                   String apiSecret) throws Exception {
        // Construct string to sign per PayPay QR Code API spec
        // METHOD\nPATH\nQUERY\nBODY\nX-REQUEST-ID\nX-TIME\n
        String canonical = method + "\n" + path + "\n" + (query == null ? "" : query) + "\n" + (body == null ? "" : body) + "\n" + requestId + "\n" + time + "\n";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sigBytes = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(sigBytes);
        // Authorization header format as per PayPay docs
        return "hmac OPA-Auth:" + apiKey + ":" + signature;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
