package com.demo.ec.controller;

import com.demo.ec.bff.EcBackendApplication;
import com.demo.ec.bff.application.auth.AuthSessionFilter;
import com.demo.ec.bff.gateway.client.OrderServiceClient;
import com.demo.ec.bff.gateway.client.dto.OrderSummary;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = EcBackendApplication.class
)
class CheckoutControllerQRCodeRestAssuredTest {

    @LocalServerPort
    int port;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @MockBean
    private AuthSessionFilter authSessionFilter;

    private static final String TEST_ORDER_ID = "ra-order-1";

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(authSessionFilter).doFilter(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(FilterChain.class));
    }

    @Test
    void returnsBase64PngQRCode_whenOrderExists_withRestAssured() {
        String paymentUrl = "https://qr-stg.sandbox.paypay.ne.jp/28180104gfrAmutBEcMIn6Rj";
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo(TEST_ORDER_ID);
        summary.setAmount(new BigDecimal("1234"));
        summary.setStatus("PAYMENT_PENDING");
        summary.setPaymentUrl(paymentUrl);
        Mockito.when(orderServiceClient.getOrder(eq(TEST_ORDER_ID)))
                .thenReturn(Optional.of(summary));

        given()
                .accept(ContentType.JSON)
        .when()
                .get("/api/payments/" + TEST_ORDER_ID + "/qrcode")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("base64Image", not(isEmptyOrNullString()))
                .body("base64Image", startsWith("iVBOR"));
    }

    @Test
    void returns404_whenOrderMissing_withRestAssured() {
        Mockito.when(orderServiceClient.getOrder(eq("unknown-id"))).thenReturn(Optional.empty());

        given()
        .when()
                .get("/api/payments/unknown-id/qrcode")
        .then()
                .statusCode(404);
    }
}
