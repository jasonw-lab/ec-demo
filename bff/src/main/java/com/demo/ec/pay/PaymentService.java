package com.demo.ec.pay;

import jp.ne.paypay.model.PaymentDetails;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PaymentService interface that defines PayPay-related payment operations.
 * Implemented by PaypayPaymentServiceImpl.
 */
public interface PaymentService {
    // High-level operations used by controller
    PaymentSession createPaymentSession(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata);
    default String createPaymentUrl(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata) {
        PaymentSession session = createPaymentSession(merchantPaymentId, amountJPY, metadata);
        if (session == null) {
            return null;
        }
        return session.getDeeplink() != null && !session.getDeeplink().isBlank()
                ? session.getDeeplink()
                : session.getPaymentUrl();
    }
    @SuppressWarnings({"rawtypes"})
    Map getPaymentDetails(String merchantPaymentId);

    // Low-level SDK operations used by tests/integration
    String createPaymentUrlUsingSdk(String merchantPaymentId, BigDecimal amountJPY, Map<String, Object> metadata);
    PaymentDetails getCodesPaymentDetailsUsingSdk(String merchantPaymentId, BigDecimal amountJPY);
}
