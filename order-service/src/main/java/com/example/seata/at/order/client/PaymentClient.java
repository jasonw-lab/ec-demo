package com.example.seata.at.order.client;

import com.example.seata.at.order.client.dto.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public PaymentClient(RestTemplate restTemplate,
                         @Value("${svc.backend.baseUrl:http://localhost:8080}") String backendBaseUrl) {
       this.restTemplate = restTemplate;
       this.backendBaseUrl = backendBaseUrl;
    }

    public PaymentResult requestPayment(String orderNo, BigDecimal amount) {
        log.info("PaymentClient.requestPayment orderNo={}, amount={}", orderNo, amount);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Order-No", orderNo);
        Map<String, Object> body = new HashMap<>();
        body.put("orderNo", orderNo);
        body.put("amount", amount);

        try {

            ResponseEntity<PaymentResult> response = restTemplate.postForEntity(
                    backendBaseUrl + "/internal/payment/paypay/pay",
                    new HttpEntity<>(body, headers),
                    PaymentResult.class
            );
            return bodyOrFailure(response, orderNo);
        } catch (RestClientException ex) {
            log.warn("PaymentClient.requestPayment failed orderNo={} err={}", orderNo, ex.toString());
            return failure("HTTP_ERROR", ex.getMessage(), orderNo);
        }
    }

    public PaymentResult getStatus(String merchantPaymentId) {
        try {
            ResponseEntity<PaymentResult> response = restTemplate.getForEntity(
                    backendBaseUrl + "/internal/payment/paypay/status/{merchantPaymentId}",
                    PaymentResult.class,
                    merchantPaymentId
            );
            return bodyOrFailure(response, merchantPaymentId);
        } catch (RestClientException ex) {
            log.warn("PaymentClient.getStatus failed merchantPaymentId={} err={}", merchantPaymentId, ex.toString());
            return failure("HTTP_ERROR", ex.getMessage(), merchantPaymentId);
        }
    }

    private static PaymentResult failure(String code, String message, String orderNo) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        result.setOrderNo(orderNo);
        return result;
    }

    private static PaymentResult bodyOrFailure(ResponseEntity<PaymentResult> response, String orderNo) {
        PaymentResult result = response.getBody();
        if (result == null) {
            log.warn("PaymentClient received null body for orderNo={}", orderNo);
            return failure("NO_RESPONSE", "Empty response from payment backend", orderNo);
        }
        if (result.getOrderNo() == null) {
            result.setOrderNo(orderNo);
        }
        return result;
    }
}
