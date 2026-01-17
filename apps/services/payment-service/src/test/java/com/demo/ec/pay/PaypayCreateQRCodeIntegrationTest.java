package com.demo.ec.pay;

import jp.ne.paypay.model.QRCodeDetails;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaypayCreateQRCodeIntegrationTest {

    @Test
    void createQRCode_callsPayPayApi_whenEnvConfigured() throws Exception {
//        Assumptions.assumeTrue(isPayPayIntegrationTestEnabled(),
//                "Set PAYPAY_TEST_ENABLED=true to run PayPay integration test");

        Map<String, String> dotenv = readDotenv();
        PayProperties payProperties = new PayProperties();
        payProperties.setEnabled(Boolean.parseBoolean(dotenv.getOrDefault("paypay.enabled", "false")));
        payProperties.setBaseUrl(dotenv.get("paypay.base-url"));
        payProperties.setMerchantId(dotenv.get("paypay.merchant-id"));
        payProperties.setApiKey(dotenv.get("paypay.api-key"));
        payProperties.setApiSecret(dotenv.get("paypay.api-secret"));
        payProperties.setCallbackUrl(dotenv.get("paypay.callback-url"));

        Assumptions.assumeTrue(payProperties.isEnabled(), "paypay.enabled must be true");
        Assumptions.assumeTrue(notBlank(payProperties.getBaseUrl()), "paypay.base-url is required");
        Assumptions.assumeTrue(notBlank(payProperties.getApiKey()), "paypay.api-key is required");
        Assumptions.assumeTrue(notBlank(payProperties.getApiSecret()), "paypay.api-secret is required");

        PaypayPaymentServiceImpl service = new PaypayPaymentServiceImpl(payProperties);

        String merchantPaymentId = "test-" + Instant.now().toEpochMilli();
        BigDecimal amount = new BigDecimal("1");
        Map<String, Object> metadata = Map.of("test", true, "source", "integration-test");

        var method = PaypayPaymentServiceImpl.class.getDeclaredMethod(
                "createQrCode",
                String.class,
                BigDecimal.class,
                Map.class
        );
        method.setAccessible(true);
        QRCodeDetails details = (QRCodeDetails) method.invoke(service, merchantPaymentId, amount, metadata);

        assertNotNull(details);
        assertNotNull(details.getResultInfo());
        assertEquals("SUCCESS", details.getResultInfo().getCode());
        assertNotNull(details.getData());
        assertTrue(
                notBlank(details.getData().getUrl()) || notBlank(details.getData().getDeeplink()),
                "data.url or data.deeplink should be present"
        );
    }

    private static boolean isPayPayIntegrationTestEnabled() {
        String value = System.getProperty("PAYPAY_TEST_ENABLED");
        if (value == null) {
            value = System.getenv("PAYPAY_TEST_ENABLED");
        }
        return Boolean.parseBoolean(value);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<String, String> readDotenv() throws Exception {
        Path path = Path.of(".env");
        if (!Files.isRegularFile(path)) {
            path = Path.of("payment-service", ".env");
        }
        Assumptions.assumeTrue(Files.isRegularFile(path), "Missing .env file (expected at repo root)");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        Map<String, String> result = new HashMap<>();
        for (String rawLine : lines) {
            if (rawLine == null) {
                continue;
            }
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            result.put(key, value);
        }
        return result;
    }
}
