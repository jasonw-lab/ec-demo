package com.demo.ec.controller;

import com.demo.ec.client.OrderServiceClient;
import com.demo.ec.client.dto.OrderSummary;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutControllerQRCodeRestAssuredTest {

    @LocalServerPort
    int port;

    @MockBean
    private OrderServiceClient orderServiceClient;

    private static final String TEST_ORDER_ID = "ra-order-1";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // no-op
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
