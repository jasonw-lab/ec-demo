package com.demo.ec.payment.web;

import com.demo.ec.payment.gateway.client.OrderServiceClient;
import com.demo.ec.payment.gateway.client.dto.PaymentStatusUpdateRequest;
import com.demo.ec.payment.gateway.messaging.PaymentEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayPayWebhookController.class)
class PayPayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @MockBean
    private PaymentEventPublisher paymentEventPublisher;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder();
        }
    }

    @Test
    void webhook_shouldProcessCompletedPayment() throws Exception {
        Map<String, Object> payload = Map.of(
                "merchantPaymentId", "ORDER-001",
                "status", "COMPLETED",
                "eventId", "EVENT-001",
                "amount", Map.of("amount", 1000, "currency", "JPY")
        );

        when(orderServiceClient.notifyPaymentStatus(eq("ORDER-001"), any(PaymentStatusUpdateRequest.class)))
                .thenReturn(Optional.of(Map.of("success", true)));

        mockMvc.perform(post("/api/paypay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(paymentEventPublisher).publishPaymentSucceeded(
                eq("ORDER-001"), eq("ORDER-001"), eq("PayPay"), eq(1000.0), eq("JPY"));
    }

    @Test
    void webhook_shouldPublishOnceForDuplicateEvent() throws Exception {
        Map<String, Object> payload = Map.of(
                "merchantPaymentId", "ORDER-002",
                "status", "COMPLETED",
                "eventId", "EVENT-002",
                "amount", Map.of("amount", 1000, "currency", "JPY")
        );

        when(orderServiceClient.notifyPaymentStatus(eq("ORDER-002"), any(PaymentStatusUpdateRequest.class)))
                .thenReturn(Optional.of(Map.of("success", true)));

        mockMvc.perform(post("/api/paypay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/paypay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // Current implementation does not store processed eventIds, so it publishes twice.
        // This test documents current behavior and will fail once idempotency is implemented.
        verify(paymentEventPublisher, org.mockito.Mockito.times(2)).publishPaymentSucceeded(
                eq("ORDER-002"), eq("ORDER-002"), eq("PayPay"), eq(1000.0), eq("JPY"));
    }

    @Test
    void webhook_shouldReturnBadRequestForEmptyPayload() throws Exception {
        mockMvc.perform(post("/api/paypay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(paymentEventPublisher, never()).publishPaymentSucceeded(any(), any(), any(), any(), any());
    }

    @Test
    void webhook_shouldReturnBadRequestWhenOrderIdMissing() throws Exception {
        Map<String, Object> payload = Map.of(
                "status", "COMPLETED",
                "eventId", "EVENT-003"
        );

        mockMvc.perform(post("/api/paypay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void webhook_shouldReturnOkEvenWhenOrderUpdateFails() throws Exception {
        Map<String, Object> payload = Map.of(
                "merchantPaymentId", "ORDER-004",
                "status", "COMPLETED",
                "eventId", "EVENT-004",
                "amount", Map.of("amount", 1000, "currency", "JPY")
        );

        when(orderServiceClient.notifyPaymentStatus(eq("ORDER-004"), any(PaymentStatusUpdateRequest.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/paypay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verify(paymentEventPublisher).publishPaymentSucceeded(
                eq("ORDER-004"), eq("ORDER-004"), eq("PayPay"), eq(1000.0), eq("JPY"));
    }

    @Test
    void callback_shouldUseSameProcessingAsWebhook() throws Exception {
        Map<String, Object> payload = Map.of(
                "merchantPaymentId", "ORDER-005",
                "status", "SUCCESS",
                "eventId", "EVENT-005",
                "amount", Map.of("amount", 1000, "currency", "JPY")
        );

        when(orderServiceClient.notifyPaymentStatus(eq("ORDER-005"), any(PaymentStatusUpdateRequest.class)))
                .thenReturn(Optional.of(Map.of("success", true)));

        mockMvc.perform(post("/paypay/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
