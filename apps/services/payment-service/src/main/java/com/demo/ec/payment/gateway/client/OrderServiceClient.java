package com.demo.ec.payment.gateway.client;

import com.demo.ec.payment.gateway.client.dto.PaymentStatusUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderServiceClient(RestTemplate restTemplate,
                              @Value("${svc.order.baseUrl:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Optional<Map> notifyPaymentStatus(String orderNo, PaymentStatusUpdateRequest request) {
        String url = baseUrl + "/api/orders/" + orderNo + "/payment/events";
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(request),
                    Map.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException ex) {
            log.error("OrderServiceClient.notifyPaymentStatus error orderNo={} err={}", orderNo, ex.toString());
            return Optional.empty();
        }
    }
}
