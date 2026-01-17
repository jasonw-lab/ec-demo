package com.demo.ec.order.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component("inventoryAction")
public class InventoryActionImpl {
    private static final Logger log = LoggerFactory.getLogger(InventoryActionImpl.class);

    private final RestTemplate restTemplate;

    private static final String STORAGE_BASE_URL = "http://localhost:8082";

    public InventoryActionImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean reduce(String businessKey, Integer count) {
        log.info("[SAGA] inventoryAction.reduce businessKey={}, count={}", businessKey, count);
        Map<String, Object> body = new HashMap<>();
        body.put("orderNo", businessKey);
        body.put("productId", 1L);
        body.put("count", count);
        Map<?, ?> res = restTemplate.postForObject(STORAGE_BASE_URL + "/api/storage/deduct/saga", body, Map.class);
        Object success = res == null ? null : res.get("success");
        return Boolean.TRUE.equals(success);
    }

    public boolean compensateReduce(String businessKey) {
        log.info("[SAGA] inventoryAction.compensateReduce businessKey={}", businessKey);
        // Storage service will handle compensation when triggered by saga engine, no direct call here
        return true;
    }
}


