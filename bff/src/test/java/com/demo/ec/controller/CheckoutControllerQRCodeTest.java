package com.demo.ec.controller;

import com.demo.ec.client.OrderServiceClient;
import com.demo.ec.client.dto.OrderSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CheckoutController.class)
class CheckoutControllerQRCodeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderServiceClient orderServiceClient;

    private static final String TEST_ORDER_ID = "test-order-1";

    @BeforeEach
    void setUp() {
        // no-op
    }

    @Test
    void returnsBase64PngQRCode_whenOrderExists() throws Exception {
        String paymentUrl = "https://qr-stg.sandbox.paypay.ne.jp/28180104gfrAmutBEcMIn6Rj";
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo(TEST_ORDER_ID);
        summary.setAmount(new BigDecimal("1234"));
        summary.setStatus("PAYMENT_PENDING");
        summary.setPaymentUrl(paymentUrl);
        Mockito.when(orderServiceClient.getOrder(eq(TEST_ORDER_ID)))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/payments/" + TEST_ORDER_ID + "/qrcode")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.base64Image", not(isEmptyString())))
                // PNG base64 typically starts with iVBOR
                .andExpect(jsonPath("$.base64Image").value(org.hamcrest.Matchers.startsWith("iVBOR")));
    }

    @Test
    void returns404_whenOrderMissing() throws Exception {
        Mockito.when(orderServiceClient.getOrder(eq("unknown-id"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/unknown-id/qrcode"))
                .andExpect(status().isNotFound());
    }
}
