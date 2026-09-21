package com.demo.ec.payment.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaypayPaymentServiceImplTest {

    @Mock
    private PayProperties properties;

    @InjectMocks
    private PaypayPaymentServiceImpl paymentService;

    @Test
    void createPaymentSession_shouldThrowWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.createPaymentSession("ORDER-001", new BigDecimal("1000"), Map.of()));
        assertEquals("PayPay integration is disabled (paypay.enabled=false)", ex.getMessage());
    }

    @Test
    void createPaymentSession_shouldThrowWhenCredentialsMissing() {
        when(properties.isEnabled()).thenReturn(true);
        // Default Mockito returns null for getApiKey/getApiSecret, which are treated as blank.

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.createPaymentSession("ORDER-002", new BigDecimal("1000"), Map.of()));
        assertTrue(ex.getMessage().contains("PayPay API credentials not configured"));
    }

    @Test
    void createPaymentUrl_shouldPropagateExceptionWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.createPaymentUrl("ORDER-003", new BigDecimal("1000"), Map.of()));
        assertEquals("PayPay integration is disabled (paypay.enabled=false)", ex.getMessage());
    }

    @Test
    void getPaymentDetails_shouldReturnNullWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Map<?, ?> result = paymentService.getPaymentDetails("ORDER-004");
        assertNull(result);
    }

    @Test
    void createPaymentUrlUsingSdk_shouldThrowWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.createPaymentUrlUsingSdk("ORDER-005", new BigDecimal("1000"), Map.of()));
        assertEquals("PayPay integration is disabled (paypay.enabled=false)", ex.getMessage());
    }

    @Test
    void getCodesPaymentDetailsUsingSdk_shouldThrowWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.getCodesPaymentDetailsUsingSdk("ORDER-006", new BigDecimal("1000")));
        assertEquals("PayPay integration is disabled (paypay.enabled=false)", ex.getMessage());
    }

    @Test
    void getCodesPaymentDetailsUsingSdk_shouldThrowWhenAmountNegative() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.getCodesPaymentDetailsUsingSdk("ORDER-007", new BigDecimal("-1")));
        assertEquals("amountJPY cannot be negative", ex.getMessage());
    }
}
