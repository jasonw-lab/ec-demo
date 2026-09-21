package com.demo.ec.bff.web;

import com.demo.ec.bff.application.auth.AuthSessionFilter;
import com.demo.ec.bff.application.auth.SessionData;
import com.demo.ec.bff.application.auth.SessionService;
import com.demo.ec.bff.config.AuthSessionProperties;
import com.demo.ec.bff.gateway.client.OrderServiceClient;
import com.demo.ec.bff.gateway.client.StorageServiceClient;
import com.demo.ec.bff.gateway.client.dto.OrderSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @MockBean
    private StorageServiceClient storageServiceClient;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private AuthSessionProperties authSessionProperties;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder();
        }
    }

    @Test
    void checkout_shouldReturnUnauthorizedWhenNoSession() throws Exception {
        String body = """
                {
                  "customerName": "Test User",
                  "customerEmail": "test@example.com",
                  "items": [{"productId": 1, "quantity": 1}]
                }
                """;

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void checkout_shouldReturnOrderWhenAuthenticated() throws Exception {
        String body = """
                {
                  "customerName": "Test User",
                  "customerEmail": "test@example.com",
                  "items": [{"productId": 1, "quantity": 1}]
                }
                """;

        StorageServiceClient.StockLookupResult stockResult =
                new StorageServiceClient.StockLookupResult(Optional.empty(), false);
        when(storageServiceClient.getStock(anyLong())).thenReturn(stockResult);
        when(storageServiceClient.getProduct(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr(AuthSessionFilter.REQ_ATTR_SESSION,
                                new SessionData("sid", "uid", 1L, List.of(), null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void purchase_shouldReturnUnauthorizedWhenNoSession() throws Exception {
        String body = """
                {
                  "customerName": "Test User",
                  "customerEmail": "test@example.com",
                  "items": [{"productId": 1, "quantity": 1}]
                }
                """;

        mockMvc.perform(post("/api/orders/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void purchase_shouldCreateOrderViaSaga() throws Exception {
        String body = """
                {
                  "customerName": "Test User",
                  "customerEmail": "test@example.com",
                  "items": [{"productId": 1, "quantity": 1}]
                }
                """;

        StorageServiceClient.StockResponse stockResponse = new StorageServiceClient.StockResponse();
        stockResponse.setProductId(1L);
        StorageServiceClient.StockLookupResult stockResult =
                new StorageServiceClient.StockLookupResult(Optional.of(stockResponse), true);
        when(storageServiceClient.getStock(anyLong())).thenReturn(stockResult);
        com.demo.ec.bff.domain.Product product =
                new com.demo.ec.bff.domain.Product(1L, 1L, "Product", "Desc", "image.jpg", new BigDecimal("1000"));
        when(storageServiceClient.getProduct(anyLong())).thenReturn(Optional.of(product));

        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-BFF-001");
        summary.setAmount(new BigDecimal("1000"));
        summary.setStatus("PENDING_PAYMENT");
        summary.setPaymentUrl("https://paypay.example.com/qr");
        when(orderServiceClient.createOrderSaga(any())).thenReturn(Optional.of(summary));

        mockMvc.perform(post("/api/orders/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr(AuthSessionFilter.REQ_ATTR_SESSION,
                                new SessionData("sid", "uid", 1L, List.of(), null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-BFF-001"))
                .andExpect(jsonPath("$.paymentUrl").value("https://paypay.example.com/qr"));
    }

    @Test
    void getPaymentDetails_shouldReturnNotFoundForUnknownOrder() throws Exception {
        when(orderServiceClient.getOrder("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/UNKNOWN/details"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentDetails_shouldReturnDetailsForExistingOrder() throws Exception {
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-BFF-002");
        summary.setAmount(new BigDecimal("2000"));
        summary.setStatus("PAID");
        when(orderServiceClient.getOrder("ORDER-BFF-002")).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/payments/ORDER-BFF-002/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-BFF-002"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getQRCode_shouldReturnConflictForTerminalStatus() throws Exception {
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-BFF-003");
        summary.setStatus("PAID");
        when(orderServiceClient.getOrder("ORDER-BFF-003")).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/payments/ORDER-BFF-003/qrcode"))
                .andExpect(status().isConflict());
    }
}
