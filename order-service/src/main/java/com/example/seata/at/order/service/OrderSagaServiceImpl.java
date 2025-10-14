package com.example.seata.at.order.service;
import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderSagaServiceImpl implements OrderSagaService {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaServiceImpl.class);

    private final OrderMapper orderMapper;
    private final RestTemplate restTemplate;
    private final StateMachineEngine stateMachineEngine;

    private static final String STORAGE_BASE_URL = "http://localhost:8082";
    private static final String ACCOUNT_BASE_URL = "http://localhost:8083";

    public OrderSagaServiceImpl(OrderMapper orderMapper, RestTemplate restTemplate, StateMachineEngine stateMachineEngine) {
        this.orderMapper = orderMapper;
        this.restTemplate = restTemplate;
        this.stateMachineEngine = stateMachineEngine;
    }

    @Override
    @GlobalTransactional(name = "order-create-saga-tx", rollbackFor = Exception.class)
    public Order createOrderSaga(OrderDTO req) {
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Start createOrderSaga: orderNo={}, userId={}, productId={}, count={}, amount={}, xid={}",
                req.getOrderNo(), req.getUserId(), req.getProductId(), req.getCount(), req.getAmount(), xid);

        // 1) 下位サービスに順次呼び出し（Saga 前提: 各サービスは補償APIを提供）
        // storage deduct (saga)
        var deductBody = new java.util.HashMap<String, Object>();
        deductBody.put("productId", req.getProductId());
        deductBody.put("count", req.getCount());
        deductBody.put("orderNo", req.getOrderNo());
        CommonResponse<?> storageRes;
        try {
            storageRes = restTemplate.postForObject(STORAGE_BASE_URL + "/api/storage/deduct/saga", deductBody, CommonResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Storage saga call failed: " + e.getMessage(), e);
        }
        if (storageRes == null || !Boolean.TRUE.equals(storageRes.isSuccess())) {
            throw new RuntimeException("Storage saga deduct failed: " + (storageRes == null ? "null" : storageRes.getMessage()));
        }

        // account debit (saga)
        var debitBody = new java.util.HashMap<String, Object>();
        debitBody.put("userId", req.getUserId());
        debitBody.put("amount", req.getAmount());
        debitBody.put("orderNo", req.getOrderNo());
        CommonResponse<?> accountRes;
        try {
            accountRes = restTemplate.postForObject(ACCOUNT_BASE_URL + "/api/account/debit/saga", debitBody, CommonResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Account saga call failed: " + e.getMessage(), e);
        }
        if (accountRes == null || !Boolean.TRUE.equals(accountRes.isSuccess())) {
            throw new RuntimeException("Account saga debit failed: " + (accountRes == null ? "null" : accountRes.getMessage()));
        }

        // 2) 注文確定
        Order order = new Order();
        order.setOrderNo(req.getOrderNo());
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setCount(req.getCount());
        order.setAmount(req.getAmount());
        order.setStatus(1);
        orderMapper.insert(order);
        log.info("createOrderSaga success: id={}, orderNo={}", order.getId(), order.getOrderNo());
        return order;
    }

    @Override
    public Order startOrderCreateSaga(OrderDTO req) {
        if (req.getOrderNo() == null || req.getOrderNo().trim().isEmpty()) {
            req.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("orderNo", req.getOrderNo());
        params.put("userId", req.getUserId());
        params.put("productId", req.getProductId());
        params.put("count", req.getCount());
        params.put("amount", req.getAmount());
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey("order_create_saga", null, req.getOrderNo(), params);
        boolean success = ExecutionStatus.SU.equals(inst.getStatus());
        if (!success) {
            throw new RuntimeException("order_create_saga failed");
        }
        Order order = new Order();
        order.setOrderNo(req.getOrderNo());
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setCount(req.getCount());
        order.setAmount(req.getAmount());
        order.setStatus(1);
        return order;
    }

    @Override
    public boolean startSampleReduceInventoryAndBalance(OrderDTO req) {
        if (req.getOrderNo() == null || req.getOrderNo().trim().isEmpty()) {
            req.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("businessKey", req.getOrderNo());
        params.put("count", req.getCount());
        params.put("amount", req.getAmount());
        params.put("mockReduceBalanceFail", false);
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey(
                "reduceInventoryAndBalance",
                null,
                req.getOrderNo(),
                params
        );
        return ExecutionStatus.SU.equals(inst.getStatus());
    }
}


