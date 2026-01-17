package com.demo.ec.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.ec.order.domain.Order;
import com.demo.ec.order.gateway.OrderMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Sample integration tests for Seata AT global transaction across services.
 * Note: These tests require the following to run locally:
 *  - Seata Server 2.x running (default localhost:8091) with file registry/config
 *  - MySQL databases with required schema and undo_log present
 *  - storage-service and account-service running on 8082 and 8083
 *
 * By default, this class is disabled to avoid CI failures. Remove @Disabled to run locally.
 */
@Disabled("Requires Seata server, MySQL, and dependent services running")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderTransactionIT {

    @LocalServerPort
    int port;

    @Autowired
    OrderMapper orderMapper;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void success_should_commit_all_and_finish_order() {
        String orderNo = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "userId", 1,
                "productId", 1,
                "count", 1,
                "amount", new BigDecimal("10.00"),
                "orderNo", orderNo
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/orders")
                .then().statusCode(200)
                .body("success", equalTo(true));

        Order saved = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertNotNull(saved);
        assertEquals(1, saved.getStatus());
    }

    @Test
    void insufficient_stock_should_rollback_and_keep_order_creating() {
        String orderNo = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "userId", 1,
                "productId", 1,
                // Intentionally large count to trigger stock failure
                "count", 999999,
                "amount", new BigDecimal("10.00"),
                "orderNo", orderNo
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/orders")
                .then().statusCode(400)
                .body("success", equalTo(false));

        // In AT mode, global tx should rollback including the order insert
        Order shouldBeNull = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertNull(shouldBeNull, "Order insert should be rolled back by Seata");
    }

    @Test
    void debit_failure_should_rollback_storage_and_order() {
        String orderNo = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                // Assume userId=2 has insufficient balance for amount below
                "userId", 2,
                "productId", 1,
                "count", 1,
                "amount", new BigDecimal("1000000.00"),
                "orderNo", orderNo
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/orders")
                .then().statusCode(400)
                .body("success", equalTo(false));

        // Both storage deduction and order insert should be rolled back
        Order shouldBeNull = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertNull(shouldBeNull);
    }

    // ===== TCC テストケース =====

    @Test
    void tcc_success_should_commit_all_and_finish_order() {
        String orderNo = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "userId", 1,
                "productId", 1,
                "count", 1,
                "amount", new BigDecimal("10.00"),
                "orderNo", orderNo
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/orders/tcc")
                .then().statusCode(200)
                .body("success", equalTo(true));

        Order saved = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertNotNull(saved);
        assertEquals(1, saved.getStatus()); // 1: 確定
    }

    @Test
    void tcc_insufficient_stock_should_rollback_and_keep_order_creating() {
        String orderNo = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "userId", 1,
                "productId", 1,
                // Intentionally large count to trigger stock failure
                "count", 999999,
                "amount", new BigDecimal("10.00"),
                "orderNo", orderNo
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/orders/tcc")
                .then().statusCode(400)
                .body("success", equalTo(false));

        // In TCC mode, global tx should rollback including the order insert
        Order shouldBeNull = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertNull(shouldBeNull, "Order insert should be rolled back by Seata TCC");
    }

    @Test
    void tcc_debit_failure_should_rollback_storage_and_order() {
        String orderNo = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                // Assume userId=2 has insufficient balance for amount below
                "userId", 2,
                "productId", 1,
                "count", 1,
                "amount", new BigDecimal("1000000.00"),
                "orderNo", orderNo
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/orders/tcc")
                .then().statusCode(400)
                .body("success", equalTo(false));

        // Both storage deduction and order insert should be rolled back
        Order shouldBeNull = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        assertNull(shouldBeNull);
    }
}
