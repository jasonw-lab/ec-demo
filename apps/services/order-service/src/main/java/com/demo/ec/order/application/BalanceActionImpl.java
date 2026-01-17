package com.demo.ec.order.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component("balanceAction")
public class BalanceActionImpl {
    private static final Logger log = LoggerFactory.getLogger(BalanceActionImpl.class);

    private final RestTemplate restTemplate;

    private static final String ACCOUNT_BASE_URL = "http://localhost:8083";

    public BalanceActionImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean reduce(String businessKey, BigDecimal amount, Map<String, Object> ext) {
        boolean throwException = ext != null && Boolean.TRUE.equals(ext.get("throwException"));
        log.info("[SAGA] balanceAction.reduce businessKey={}, amount={}, throwException={}", businessKey, amount, throwException);
        if (throwException) {
            throw new RuntimeException("Mock reduce balance failure");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("orderNo", businessKey);
        body.put("userId", 1L);
        body.put("amount", amount);
        Map<?, ?> res = restTemplate.postForObject(ACCOUNT_BASE_URL + "/api/account/debit/saga", body, Map.class);
        Object success = res == null ? null : res.get("success");
        return Boolean.TRUE.equals(success);
    }

    public boolean compensateReduce(String businessKey) {
        log.info("[SAGA] balanceAction.compensateReduce businessKey={}", businessKey);
        // Account service will handle compensation when triggered by saga engine, no direct call here
        return true;
    }
}


