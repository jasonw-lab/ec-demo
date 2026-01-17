package com.demo.ec.order;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class OrderSagaIT {

    @Test
    @Disabled("Requires running services and Seata server")
    void createOrderSaga_success() {
        Map<String, Object> body = new HashMap<>();
        body.put("orderNo", java.util.UUID.randomUUID().toString());
        body.put("userId", 1);
        body.put("productId", 1);
        body.put("count", 1);
        body.put("amount", new BigDecimal("1"));

        Response res = RestAssured.given()
                .contentType("application/json")
                .body(body)
                .post("http://localhost:8081/api/orders/saga");

        assertThat(res.statusCode(), equalTo(200));
        assertThat(res.jsonPath().getBoolean("success"), equalTo(true));
    }
}


