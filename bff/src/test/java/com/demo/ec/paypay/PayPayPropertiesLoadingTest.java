package com.demo.ec.paypay;

import com.demo.ec.pay.PayProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

class PayPayPropertiesLoadingTest {

    private static final Logger log = LoggerFactory.getLogger(PayPayPropertiesLoadingTest.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            // simulate defaults from application.properties (no env provided)
            .withPropertyValues(
                    "paypay.enabled=true",
                    "paypay.base-url=https://stg-api.paypay.ne.jp",
                    "paypay.callback-url=http://localhost:5173/checkout",
                    "paypay.merchant-id=",
                    "paypay.api-key=",
                    "paypay.api-secret="
            );

    @Test
    void loadsFromApplicationProperties() {
        contextRunner.run(context -> {
            PayProperties properties = context.getBean(PayProperties.class);
            assertNotNull(properties, "PayPayProperties should be loaded as a bean");
            assertTrue(properties.isEnabled(), "paypay.enabled should default to true");
            assertEquals("https://stg-api.paypay.ne.jp", properties.getBaseUrl(), "paypay.base-url default mismatch");
            // The following are empty by default (no env provided)
            log.info("merchantId={}", properties.getMerchantId());
            log.info("apiKey={}", properties.getApiKey());
            log.info("apiSecret={}", properties.getApiSecret());
            assertEquals("http://localhost:5173/checkout", properties.getCallbackUrl(), "paypay.callback-url default mismatch");
        });
    }

    @Configuration
    @EnableConfigurationProperties(PayProperties.class)
    static class TestConfig {}
}
