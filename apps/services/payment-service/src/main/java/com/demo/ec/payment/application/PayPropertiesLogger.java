package com.demo.ec.payment.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PayPropertiesLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PayPropertiesLogger.class);
    private final PayProperties payProperties;

    public PayPropertiesLogger(PayProperties payProperties) {
        this.payProperties = payProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String merchantId = payProperties.getMerchantId();
        String callbackUrl = payProperties.getCallbackUrl();

        if (!StringUtils.hasText(merchantId)) {
            log.warn("PayPay merchantId is not set (property: paypay.merchant-id)");
        } else {
            log.info("PayPay merchantId: {}", merchantId);
        }

        if (!StringUtils.hasText(callbackUrl)) {
            log.warn("PayPay callbackUrl is not set (property: paypay.callback-url)");
        } else {
            log.info("PayPay callbackUrl: {}", callbackUrl);
        }
    }
}


