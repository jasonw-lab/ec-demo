package com.example.seata.at.account;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"seata.enabled=false", "spring.main.lazy-initialization=true"})
class HealthApiTest {

    @LocalServerPort
    int port;

    @Test
    void healthShouldReturnStatusUp() {
        RestAssured.given()
                .baseUri("http://localhost")
                .port(port)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
