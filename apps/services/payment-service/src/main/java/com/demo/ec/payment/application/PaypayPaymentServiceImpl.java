package com.demo.ec.payment.application;

import jp.ne.paypay.ApiClient;
import jp.ne.paypay.ApiException;
import jp.ne.paypay.api.PaymentApi;
import jp.ne.paypay.model.MoneyAmount;
import jp.ne.paypay.model.QRCode;
import jp.ne.paypay.model.QRCodeDetails;
import jp.ne.paypay.model.QRCodeResponse;
import jp.ne.paypay.model.PaymentDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of PaymentService using the official PayPay Java SDK.
 */
@Service
public class PaypayPaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaypayPaymentServiceImpl.class);

    private final PayProperties properties;

    public PaypayPaymentServiceImpl(PayProperties properties) {
        this.properties = properties;
    }

    // High-level controller operation: delegate to SDK
    @Override
    public String createPaymentUrl(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) {
        PaymentSession session = createPaymentSession(merchantPaymentId, amountJPY, metadata);
        if (session == null) {
            return null;
        }
        return session.getDeeplink() != null && !session.getDeeplink().isBlank()
                ? session.getDeeplink()
                : session.getPaymentUrl();
    }

    @Override
    public PaymentSession createPaymentSession(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) {
        Objects.requireNonNull(merchantPaymentId, "merchantPaymentId cannot be null");
        Objects.requireNonNull(amountJPY, "amountJPY cannot be null");

        if (!properties.isEnabled()) {
            throw new IllegalStateException("PayPay integration is disabled (paypay.enabled=false)");
        }
        if (isBlank(properties.getApiKey()) || isBlank(properties.getApiSecret())) {
            throw new IllegalStateException("PayPay API credentials not configured. Set paypay.api-key and paypay.api-secret");
        }

        Map<String, Object> meta = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        meta.putIfAbsent("source", "ec-demo");

        try {
            QRCodeDetails details = createQrCode(merchantPaymentId, amountJPY, meta);
            PaymentSession session = toPaymentSession(merchantPaymentId, details);
            log.info("PayPay session created merchantPaymentId={} paymentUrl={} expiresAt={}",
                    merchantPaymentId, session.getPaymentUrl(), session.getExpiresAt());
            return session;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create PayPay payment session: {}", e.getMessage(), e);
            throw new RuntimeException("PayPay SDK call failed: " + e.getMessage(), e);
        }
    }

    // High-level controller operation: fetch details and shape into Map
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map getPaymentDetails(String merchantPaymentId) {
        if (!properties.isEnabled()) {
            log.warn("PaymentService.getPaymentDetails called but integration disabled");
            return null;
        }
        try {
            PaymentApi api = new PaymentApi(buildClient());
            // Use codes-specific endpoint for Dynamic QR payments
            jp.ne.paypay.model.PaymentDetails details = api.getCodesPaymentDetails(merchantPaymentId);

            Map result = new HashMap();
            
            // Check for error in result info
            if (details != null && details.getResultInfo() != null) {
                String resultCode = details.getResultInfo().getCode();
                String resultMessage = details.getResultInfo().getMessage();
                log.info("PayPay getPaymentDetails: resultCode={}, resultMessage={}", resultCode, resultMessage);
                
                // Handle specific error codes
                if (resultCode != null && !"SUCCESS".equals(resultCode)) {
                    log.warn("PayPay API returned error code: {} - {}", resultCode, resultMessage);
                    result.put("error", Map.of("code", resultCode, "message", resultMessage));
                    return result;
                }
            }
            
            if (details != null && details.getData() != null) {
                jp.ne.paypay.model.Payment payment = details.getData();
                jp.ne.paypay.model.PaymentState.StatusEnum status = payment.getStatus();
                Map dataMap = new HashMap();
                if (status != null) {
                    dataMap.put("status", status.name());
                }
                result.put("data", dataMap);
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.error("Failed to fetch PayPay payment details: {}", e.getMessage(), e);
            return Map.of("error", Map.of("code", "UNKNOWN", "message", e.getMessage()));
        }
    }

    // SDK operations used by tests/integration
    @Override
    public String createPaymentUrlUsingSdk(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) {
        PaymentSession session = createPaymentSession(merchantPaymentId, amountJPY, metadata);
        String url = session.getDeeplink() != null && !session.getDeeplink().isBlank()
                ? session.getDeeplink()
                : session.getPaymentUrl();
        if (url == null) {
            throw new IllegalStateException("PayPay SDK did not return a URL for QR Code");
        }
        return url;
    }

    @Override
    public PaymentDetails getCodesPaymentDetailsUsingSdk(String merchantPaymentId, BigDecimal amountJPY) {
        Objects.requireNonNull(merchantPaymentId, "merchantPaymentId cannot be null");
        Objects.requireNonNull(amountJPY, "amountJPY cannot be null");
        if (amountJPY.signum() < 0) {
            throw new IllegalArgumentException("amountJPY cannot be negative");
        }
        log.info("getCodesPaymentDetailsUsingSdk called with merchantPaymentId={}, amountJPY={}", merchantPaymentId, amountJPY);

        if (!properties.isEnabled()) {
            throw new IllegalStateException("PayPay integration is disabled (paypay.enabled=false)");
        }
        if (isBlank(properties.getApiKey()) || isBlank(properties.getApiSecret())) {
            throw new IllegalStateException("PayPay API credentials not configured. Set paypay.api-key and paypay.api-secret");
        }

        try {
            ApiClient client = new ApiClient();
            if (!isBlank(properties.getBaseUrl())) {
                client.setBasePath(properties.getBaseUrl());
            }
            client.setApiKey(properties.getApiKey());
            client.setApiSecretKey(properties.getApiSecret());
            if (!isBlank(properties.getMerchantId())) {
                client.setAssumeMerchant(properties.getMerchantId());
            }

            PaymentApi paymentApi = new PaymentApi(client);
            PaymentDetails response = paymentApi.getCodesPaymentDetails(merchantPaymentId);
            if (response != null) {
                try {
                    String code = response.getResultInfo() != null ? response.getResultInfo().getCode() : null;
                    String status = response.getData() != null && response.getData().getStatus() != null ? response.getData().getStatus().name() : null;
                    log.info("PayPay getCodesPaymentDetails: resultCode={}, status={}", code, status);
                } catch (Exception ignore) {
                    // ignore logging issues
                }
            }
            return response;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get PayPay payment details via SDK: {}", e.getMessage(), e);
            throw new RuntimeException("PayPay SDK call failed: " + e.getMessage(), e);
        }
    }

    private QRCodeDetails createQrCode(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) throws Exception {
        PaymentApi paymentApi = new PaymentApi(buildClient());

        MoneyAmount amount = new MoneyAmount()
                .amount(amountJPY.intValue())
                .currency(MoneyAmount.CurrencyEnum.JPY);

        QRCode req = new QRCode()
                .merchantPaymentId(merchantPaymentId)
                .codeType("ORDER_QR")
                .amount(amount)
                .metadata(metadata)
                .requestedAt(System.currentTimeMillis() / 1000L);

        if (!isBlank(properties.getCallbackUrl())) {
            req.redirectUrl(properties.getCallbackUrl());
            req.redirectType(QRCode.RedirectTypeEnum.WEB_LINK);
        }

        QRCodeDetails details = paymentApi.createQRCode(req);
        log.debug("PayPay createQRCode response: {}", details);
        validateResultInfo(details);
        return details;
    }

    private PaymentSession toPaymentSession(String merchantPaymentId, QRCodeDetails details) {
        if (details == null) {
            throw new IllegalStateException("PayPay SDK did not return QR code details");
        }
        QRCodeResponse data = details.getData();
        if (data == null) {
            throw new IllegalStateException("PayPay SDK response missing data payload");
        }
        String deeplink = safeString(data.getDeeplink());
        String qrUrl = safeString(data.getUrl());
        String primary = qrUrl != null ? qrUrl : deeplink;
        if (primary == null) {
            throw new IllegalStateException("PayPay SDK did not provide deeplink or url");
        }
        Instant expiresAt = extractExpiryInstant(data);
        return new PaymentSession(merchantPaymentId, primary, deeplink, expiresAt);
    }

    private void validateResultInfo(QRCodeDetails details) {
        if (details != null && details.getResultInfo() != null) {
            String resultCode = details.getResultInfo().getCode();
            String resultMessage = details.getResultInfo().getMessage();
            log.info("PayPay createQRCode result: code={}, message={}", resultCode, resultMessage);
            if (resultCode != null && !"SUCCESS".equalsIgnoreCase(resultCode)) {
                throw new IllegalStateException("PayPay API returned error code: " + resultCode + " - " + resultMessage);
            }
        }
    }

    private Instant extractExpiryInstant(QRCodeResponse data) {
        try {
            Object expiry = data.getExpiryDate();
            return toInstant(expiry);
        } catch (Exception ex) {
            log.debug("Failed to extract expiry from QRCodeResponse: {}", ex.getMessage());
            return null;
        }
    }

    private ApiClient buildClient() {
        try {
            ApiClient client = new ApiClient();
            if (!isBlank(properties.getBaseUrl())) {
                client.setBasePath(properties.getBaseUrl());
            }
            client.setApiKey(properties.getApiKey());
            client.setApiSecretKey(properties.getApiSecret());
            if (!isBlank(properties.getMerchantId())) {
                client.setAssumeMerchant(properties.getMerchantId());
            }
            return client;
        } catch (ApiException e) {
            throw new IllegalStateException("Failed to configure PayPay ApiClient: " + e.getMessage(), e);
        }
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Number number) {
            long epoch = number.longValue();
            return epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            try {
                return Instant.parse(trimmed);
            } catch (Exception ignored) {
                try {
                    long epoch = Long.parseLong(trimmed);
                    return epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
                } catch (NumberFormatException ignored2) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String safeString(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
