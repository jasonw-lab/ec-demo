package com.demo.ec.paypay;

import com.demo.ec.controller.CheckoutController;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PayPayPropertiesLoadingTest {

    @Autowired
    private PayPayProperties properties;

    private static final Logger log = LoggerFactory.getLogger(PayPayPropertiesLoadingTest.class);

    @Test
    void loadsFromApplicationProperties() {
        assertNotNull(properties, "PayPayProperties should be loaded as a bean");
        // Defaults come from src/main/resources/application.properties when no env vars provided
        assertTrue(properties.isEnabled(), "paypay.enabled should default to true");
        assertEquals("https://stg-api.paypay.ne.jp", properties.getBaseUrl(), "paypay.base-url default mismatch");

        
        // The following are empty by default (no env provided)
        log.info("merchantId={}", properties.getMerchantId());
        log.info("apiKey={}", properties.getApiKey());
        log.info("apiSecret={}", properties.getApiSecret());

        assertEquals("http://localhost:5173/checkout", properties.getCallbackUrl(), "paypay.callback-url default mismatch");
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
