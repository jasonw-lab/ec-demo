package com.example.seata.at.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class StorageClient {

    private static final Logger log = LoggerFactory.getLogger(StorageClient.class);

    private final RestTemplate restTemplate;
    private final String storageBaseUrl;

    public StorageClient(RestTemplate restTemplate,
                         @Value("${svc.storage.baseUrl:http://localhost:8082}") String storageBaseUrl) {
        this.restTemplate = restTemplate;
        this.storageBaseUrl = storageBaseUrl;
    }

    public boolean deduct(String orderNo, Long productId, Integer count) {
        return post(orderNo, productId, count, "/api/storage/deduct/saga");
    }

    public boolean compensate(String orderNo, Long productId, Integer count) {
        return post(orderNo, productId, count, "/api/storage/compensate/saga");
    }

    @SuppressWarnings("unchecked")
    private boolean post(String orderNo, Long productId, Integer count, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Order-No", orderNo);

        Map<String, Object> body = new HashMap<>();
        body.put("orderNo", orderNo);
        body.put("productId", productId);
        body.put("count", count);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    storageBaseUrl + path,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<String, Object> result = response.getBody();
            Object success = result == null ? null : result.get("success");
            boolean ok = Boolean.TRUE.equals(success);
            if (!ok) {
                log.warn("StorageClient.post failed orderNo={} path={} resp={}", orderNo, path, result);
            }
            return ok;
        } catch (RestClientException ex) {
            log.warn("StorageClient.post exception orderNo={} path={} err={}", orderNo, path, ex.toString());
            return false;
        }
    }
}
