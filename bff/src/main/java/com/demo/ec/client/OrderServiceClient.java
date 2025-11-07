package com.demo.ec.client;

import com.demo.ec.client.dto.OrderServiceRequest;
import com.demo.ec.client.dto.OrderServiceResponse;
import com.demo.ec.client.dto.OrderSummary;
import com.demo.ec.client.dto.PaymentStatusUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Optional;

@Component
public class OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderServiceClient(RestTemplate restTemplate,
                              @Value("${services.order.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public Optional<OrderSummary> createOrderSaga(OrderServiceRequest request) {
        String url = baseUrl + "/api/orders/saga";
        try {
            ParameterizedTypeReference<OrderServiceResponse<OrderSummary>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<OrderServiceResponse<OrderSummary>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    type
            );
            OrderServiceResponse<OrderSummary> body = response.getBody();
            if (body == null) {
                log.warn("OrderServiceClient.createOrderSaga empty response");
                return Optional.empty();
            }
            if (!body.isSuccess() && body.getData() == null) {
                log.warn("OrderServiceClient.createOrderSaga failed message={}", body.getMessage());
                return Optional.empty();
            }
            return Optional.ofNullable(body.getData());
        } catch (RestClientException ex) {
            log.error("OrderServiceClient.createOrderSaga error {}", ex.toString());
            return Optional.empty();
        }
    }

    public Optional<OrderSummary> getOrder(String orderNo) {
        String url = baseUrl + "/api/orders/" + orderNo;
        try {
            ParameterizedTypeReference<OrderServiceResponse<OrderSummary>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<OrderServiceResponse<OrderSummary>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    type
            );
            OrderServiceResponse<OrderSummary> body = response.getBody();
            if (body == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(body.getData());
        } catch (RestClientException ex) {
            log.error("OrderServiceClient.getOrder error orderNo={} err={}", orderNo, ex.toString());
            return Optional.empty();
        }
    }

    public Optional<OrderSummary> notifyPaymentStatus(String orderNo, PaymentStatusUpdateRequest request) {
        String url = baseUrl + "/api/orders/" + orderNo + "/payment/events";
        try {
            ParameterizedTypeReference<OrderServiceResponse<OrderSummary>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<OrderServiceResponse<OrderSummary>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    type
            );
            OrderServiceResponse<OrderSummary> body = response.getBody();
            if (body == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(body.getData());
        } catch (RestClientException ex) {
            log.error("OrderServiceClient.notifyPaymentStatus error orderNo={} err={}", orderNo, ex.toString());
            return Optional.empty();
        }
    }
}
