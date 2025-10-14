package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component("orderSagaActions")
public class OrderSagaActions {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaActions.class);

    private final RestTemplate restTemplate;
    private final OrderMapper orderMapper;

    private static final String STORAGE_BASE_URL = "http://localhost:8082";
    private static final String ACCOUNT_BASE_URL = "http://localhost:8083";

    public OrderSagaActions(RestTemplate restTemplate, OrderMapper orderMapper) {
        this.restTemplate = restTemplate;
        this.orderMapper = orderMapper;
    }

    // Service Tasks
    public boolean storageDeduct(Map<String, Object> ctx) {
        String orderNo = (String) ctx.get("orderNo");
        Long productId = toLong(ctx.get("productId"));
        Integer count = toInt(ctx.get("count"));
        log.info("[SAGA] storageDeduct orderNo={}, productId={}, count={}", orderNo, productId, count);
        var body = new java.util.HashMap<String, Object>();
        body.put("productId", productId);
        body.put("count", count);
        body.put("orderNo", orderNo);
        var res = restTemplate.postForObject(STORAGE_BASE_URL + "/api/storage/deduct/saga", body, Map.class);
        return res != null && Boolean.TRUE.equals(res.get("success"));
    }

    public boolean accountDebit(Map<String, Object> ctx) {
        String orderNo = (String) ctx.get("orderNo");
        Long userId = toLong(ctx.get("userId"));
        BigDecimal amount = toBigDecimal(ctx.get("amount"));
        log.info("[SAGA] accountDebit orderNo={}, userId={}, amount={}", orderNo, userId, amount);
        var body = new java.util.HashMap<String, Object>();
        body.put("userId", userId);
        body.put("amount", amount);
        body.put("orderNo", orderNo);
        var res = restTemplate.postForObject(ACCOUNT_BASE_URL + "/api/account/debit/saga", body, Map.class);
        return res != null && Boolean.TRUE.equals(res.get("success"));
    }

    public boolean createOrder(Map<String, Object> ctx) {
        String orderNo = (String) ctx.get("orderNo");
        Long userId = toLong(ctx.get("userId"));
        Long productId = toLong(ctx.get("productId"));
        Integer count = toInt(ctx.get("count"));
        BigDecimal amount = toBigDecimal(ctx.get("amount"));
        log.info("[SAGA] createOrder orderNo={}", orderNo);
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setCount(count);
        order.setAmount(amount);
        order.setStatus(1);
        orderMapper.insert(order);
        return true;
    }

    // Compensations
    public boolean compensateStorageDeduct(Map<String, Object> ctx) {
        String orderNo = (String) ctx.get("orderNo");
        Long productId = toLong(ctx.get("productId"));
        Integer count = toInt(ctx.get("count"));
        log.info("[SAGA] compensateStorageDeduct orderNo={}, productId={}, count={}", orderNo, productId, count);
        var body = new java.util.HashMap<String, Object>();
        body.put("orderNo", orderNo);
        body.put("productId", productId);
        body.put("count", count);
        Map<?, ?> res = restTemplate.postForObject(STORAGE_BASE_URL + "/api/storage/compensate/saga", body, Map.class);
        Object success = res == null ? null : res.get("success");
        return Boolean.TRUE.equals(success);
    }

    public boolean compensateAccountDebit(Map<String, Object> ctx) {
        String orderNo = (String) ctx.get("orderNo");
        Long userId = toLong(ctx.get("userId"));
        BigDecimal amount = toBigDecimal(ctx.get("amount"));
        log.info("[SAGA] compensateAccountDebit orderNo={}, userId={}, amount={}", orderNo, userId, amount);
        var body = new java.util.HashMap<String, Object>();
        body.put("orderNo", orderNo);
        body.put("userId", userId);
        body.put("amount", amount);
        Map<?, ?> res = restTemplate.postForObject(ACCOUNT_BASE_URL + "/api/account/compensate/saga", body, Map.class);
        Object success = res == null ? null : res.get("success");
        return Boolean.TRUE.equals(success);
    }

    public boolean compensateCreateOrder(Map<String, Object> ctx) {
        String orderNo = (String) ctx.get("orderNo");
        log.info("[SAGA] compensateCreateOrder orderNo={}", orderNo);
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getOrderNo, orderNo).set(Order::getStatus, 2);
        orderMapper.update(null, uw);
        return true;
    }

    private Long toLong(Object v) { return v == null ? null : Long.valueOf(String.valueOf(v)); }
    private Integer toInt(Object v) { return v == null ? null : Integer.valueOf(String.valueOf(v)); }
    private java.math.BigDecimal toBigDecimal(Object v) { return v == null ? null : new java.math.BigDecimal(String.valueOf(v)); }
}



