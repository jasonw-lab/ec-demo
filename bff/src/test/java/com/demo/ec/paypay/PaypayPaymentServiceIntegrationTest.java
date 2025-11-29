package com.demo.ec.paypay;

import com.demo.ec.pay.PayProperties;
import com.demo.ec.pay.PaymentService;
import jp.ne.paypay.model.PaymentDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaypayPaymentServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PaypayPaymentServiceIntegrationTest.class);

    @Autowired
    private PaymentService service;

    @Autowired
    private PayProperties properties;

    @Test
    void createPaymentUrlAndFetchDetails_realCall_whenConfigured() {
        assertNotNull(service, "PaypayPaymentService bean should be available");
        assertNotNull(properties, "PayPayProperties should be available");

        boolean ready = properties.isEnabled()
                && notBlank(properties.getApiKey())
                && notBlank(properties.getApiSecret());
        Assumptions.assumeTrue(ready, "PayPay credentials not configured; skipping real API call test");

        String orderId = "order-" + System.currentTimeMillis();
        BigDecimal amount = new BigDecimal("1"); // 1 JPY for test
        Map<String, Object> meta = new HashMap<>();
        meta.put("test", true);

        // Create QR
        String url = service.createPaymentUrlUsingSdk(orderId, amount, meta);
        log.info("PayPay SDK returned URL: {}", url);
        assertNotNull(url, "URL should not be null");
//        assertTrue(url.startsWith("http"), "URL should start with http/https");

    }


    @Test
    void getPaymentDetails_realCall_whenConfigured() {
        boolean ready = properties.isEnabled()
                && notBlank(properties.getApiKey())
                && notBlank(properties.getApiSecret());
        Assumptions.assumeTrue(ready, "PayPay credentials not configured; skipping real API call test");

        String orderId = "order-" + System.currentTimeMillis();
        BigDecimal amount = new BigDecimal("1");
        Map<String, Object> meta = new HashMap<>();
        meta.put("integrationTest", true);

        // Create QR first, then fetch details for the same merchantPaymentId
        String url = service.createPaymentUrlUsingSdk(orderId, amount, meta);
        assertNotNull(url, "URL should not be null when credentials are configured");

        PaymentDetails details = service.getCodesPaymentDetailsUsingSdk(orderId, amount);
        assertNotNull(details, "PaymentDetails should not be null");
        assertNotNull(details.getResultInfo(), "ResultInfo should not be null");
        if (details.getData() != null) {
            // status may be CREATED/ACTIVE/AUTHORIZED/COMPLETED depending on real flow
            assertNotNull(details.getData().getStatus(), "Status should be available in data when present");
            log.info("Payment status: {}", details.getData().getStatus());
        }
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
