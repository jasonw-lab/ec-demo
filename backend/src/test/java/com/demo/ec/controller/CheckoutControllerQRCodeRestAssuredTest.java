package com.demo.ec.controller;

import com.demo.ec.pay.PaymentService;
import com.demo.ec.repo.DemoData;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutControllerQRCodeRestAssuredTest {

    @LocalServerPort
    int port;

    @MockBean
    private PaymentService payPayService;

    private static final String TEST_ORDER_ID = "ra-order-1";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // Prepare a demo order in in-memory store
        Map<String, Object> order = new HashMap<>();
        order.put("id", TEST_ORDER_ID);
        order.put("amount", new BigDecimal("1234"));
        order.put("currency", "JPY");
        order.put("status", "PENDING_PAYMENT");
        DemoData.orders.put(TEST_ORDER_ID, order);
    }

    @Test
    void returnsBase64PngQRCode_whenOrderExists_withRestAssured() {
        String paymentUrl = "https://qr-stg.sandbox.paypay.ne.jp/28180104gfrAmutBEcMIn6Rj";
        Mockito.when(payPayService.createPaymentUrl(eq(TEST_ORDER_ID), eq(new BigDecimal("1234")), any(Map.class)))
                .thenReturn(paymentUrl);

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
        given()
        .when()
                .get("/api/payments/unknown-id/qrcode")
        .then()
                .statusCode(404);
    }
}
