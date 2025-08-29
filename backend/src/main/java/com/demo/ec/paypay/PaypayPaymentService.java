package com.demo.ec.paypay;

import jp.ne.paypay.ApiClient;
import jp.ne.paypay.api.PaymentApi;
import jp.ne.paypay.model.MoneyAmount;
import jp.ne.paypay.model.QRCode;
import jp.ne.paypay.model.QRCodeDetails;
import jp.ne.paypay.model.QRCodeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Service responsible for interacting with PayPay Java SDK to create Dynamic QR Codes.
 * This class is intended to call the real PayPay API (no simulator).
 */
@Service
public class PaypayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaypayPaymentService.class);

    private final PayPayProperties properties;

    public PaypayPaymentService(PayPayProperties properties) {
        this.properties = properties;
    }

    /**
     * Create a Dynamic QR Code payment URL using PayPay Official Java SDK.
     * Note: This method will attempt to call PayPay server only when credentials are configured and paypay.enabled=true.
     *
     * @param merchantPaymentId unique order id (merchant side)
     * @param amountJPY amount in JPY
     * @param metadata any metadata to attach (optional)
     * @return URL that can be opened by the user to complete payment
     */
    public String createPaymentUrlUsingSdk(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) {
        Objects.requireNonNull(merchantPaymentId, "merchantPaymentId cannot be null");
        Objects.requireNonNull(amountJPY, "amountJPY cannot be null");

        if (!properties.isEnabled()) {
            throw new IllegalStateException("PayPay integration is disabled (paypay.enabled=false)");
        }
        if (isBlank(properties.getApiKey()) || isBlank(properties.getApiSecret())) {
            throw new IllegalStateException("PayPay API credentials not configured. Set paypay.api-key and paypay.api-secret");
        }

        // Default metadata holder
        Map<String, Object> meta = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        meta.putIfAbsent("source", "ec-demo");

        try {
            // ---- Using PayPay SDK (paypayopa-sdk-java) ----
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

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
