package com.demo.ec.pay;

import jp.ne.paypay.ApiClient;
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
        return createPaymentUrlUsingSdk(merchantPaymentId, amountJPY, metadata);
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
            ApiClient client = new ApiClient();
            if (!isBlank(properties.getBaseUrl())) {
                client.setBasePath(properties.getBaseUrl());
            }
            client.setApiKey(properties.getApiKey());
            client.setApiSecretKey(properties.getApiSecret());
            if (!isBlank(properties.getMerchantId())) {
                client.setAssumeMerchant(properties.getMerchantId());
            }

            PaymentApi api = new PaymentApi(client);
            jp.ne.paypay.model.PaymentDetails details = api.getPaymentDetails(merchantPaymentId);

            Map result = new HashMap();
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
            return null;
        }
    }

    // SDK operations used by tests/integration
    @Override
    public String createPaymentUrlUsingSdk(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) {
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

            MoneyAmount amount = new MoneyAmount()
                    .amount(amountJPY.intValue())
                    .currency(MoneyAmount.CurrencyEnum.JPY);

            QRCode req = new QRCode()
                    .merchantPaymentId(merchantPaymentId)
                    .codeType("ORDER_QR")
                    .amount(amount)
                    .metadata(meta);

            QRCodeDetails details = paymentApi.createQRCode(req);
            log.info("PayPay SDK returned QR Code details: {}", details);
            String url = null;
            if (details != null) {
                QRCodeResponse data = details.getData();
                if (data != null) {
                    if (data.getUrl() != null) {
                        url = data.getUrl();
                    } else if (data.getDeeplink() != null) {
                        url = data.getDeeplink();
                    }
                }
            }
            if (url == null) {
                throw new IllegalStateException("PayPay SDK did not return a URL for QR Code");
            }
            return url;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create PayPay Dynamic QR Code via SDK: {}", e.getMessage(), e);
            throw new RuntimeException("PayPay SDK call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentDetails getCodesPaymentDetailsUsingSdk(String merchantPaymentId) {
        Objects.requireNonNull(merchantPaymentId, "merchantPaymentId cannot be null");

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

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
