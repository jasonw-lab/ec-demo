package com.demo.ec.paypay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
    private PaypayPaymentService service;

    @Autowired
    private PayPayProperties properties;

    @Test
    void createPaymentUrlUsingSdk_realCall_whenConfigured() {
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

        String url = service.createPaymentUrlUsingSdk(orderId, amount, meta);
        log.info("PayPay SDK returned URL: {}", url);
        assertNotNull(url, "URL should not be null");
        assertTrue(url.startsWith("http"), "URL should start with http/https");
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
