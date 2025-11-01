package com.demo.ec.controller;

import com.demo.ec.pay.PaymentService;
import com.demo.ec.repo.DemoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CheckoutController.class)
class CheckoutControllerQRCodeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService payPayService;

    private static final String TEST_ORDER_ID = "test-order-1";

    @BeforeEach
    void setUp() {
        // Prepare a demo order in in-memory store
        Map<String, Object> order = new HashMap<>();
        order.put("id", TEST_ORDER_ID);
        order.put("amount", new BigDecimal("1234"));
        order.put("currency", "JPY");
        order.put("status", "PENDING_PAYMENT");
        DemoData.orders.put(TEST_ORDER_ID, order);
    }

    @Test
    void returnsBase64PngQRCode_whenOrderExists() throws Exception {
        String paymentUrl = "https://qr-stg.sandbox.paypay.ne.jp/28180104gfrAmutBEcMIn6Rj";
        Mockito.when(payPayService.createPaymentUrl(eq(TEST_ORDER_ID), eq(new BigDecimal("1234")), any(Map.class)))
                .thenReturn(paymentUrl);

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
        mockMvc.perform(get("/api/payments/unknown-id/qrcode"))
                .andExpect(status().isNotFound());
    }
}
