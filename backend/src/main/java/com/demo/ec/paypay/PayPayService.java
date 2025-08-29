package com.demo.ec.paypay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Facade service used by controllers. Delegates to PaypayPaymentService and provides basic
 * payment detail lookup using the official PayPay Java SDK when enabled.
 */
@Service
public class PayPayService {

    private static final Logger log = LoggerFactory.getLogger(PayPayService.class);

    private final PaypayPaymentService paypayPaymentService;
    private final PayPayProperties properties;

    public PayPayService(PaypayPaymentService paypayPaymentService, PayPayProperties properties) {
        this.paypayPaymentService = paypayPaymentService;
        this.properties = properties;
    }

    /**
     * Create payment URL for given merchant payment id using PayPay SDK.
     */
    public String createPaymentUrl(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) {
        return paypayPaymentService.createPaymentUrlUsingSdk(merchantPaymentId, amountJPY, metadata);
    }

    /**
     * Fetch payment details by merchantPaymentId using PayPay SDK.
     * Returns a Map shaped roughly like { "data": { "status": "..." } } so that controller can read it.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map getPaymentDetails(String merchantPaymentId) {
        if (!properties.isEnabled()) {
            log.warn("PayPayService.getPaymentDetails called but integration disabled");
            return null;
        }
        try {
            jp.ne.paypay.ApiClient client = new jp.ne.paypay.ApiClient();
            if (notBlank(properties.getBaseUrl())) {
                client.setBasePath(properties.getBaseUrl());
            }
            client.setApiKey(properties.getApiKey());
            client.setApiSecretKey(properties.getApiSecret());
            if (notBlank(properties.getMerchantId())) {
                client.setAssumeMerchant(properties.getMerchantId());
            }

            jp.ne.paypay.api.PaymentApi api = new jp.ne.paypay.api.PaymentApi(client);
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

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
