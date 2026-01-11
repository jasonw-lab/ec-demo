package com.example.seata.at.order.service;
import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderSagaServiceImpl implements OrderSagaService {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaServiceImpl.class);

    private final OrderMapper orderMapper;
    private final StateMachineEngine stateMachineEngine;

    public OrderSagaServiceImpl(OrderMapper orderMapper, StateMachineEngine stateMachineEngine) {
        this.orderMapper = orderMapper;
        this.stateMachineEngine = stateMachineEngine;
    }

    @Override
    // @GlobalTransactional removed: conflicts with StateMachineEngine's Saga compensation mechanism.
    // Saga manages distributed transactions through its own state machine and compensation logic.
    public Order createOrderSaga(OrderDTO req) {
        return startOrderCreateSaga(req);
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
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey("order_initialization_saga", null, req.getOrderNo(), params);
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, req.getOrderNo()));
        if (order == null) {
            log.warn("order_initialization_saga finished but order not found orderNo={} status={}", req.getOrderNo(), inst.getStatus());
            throw new RuntimeException("order_initialization_saga failed: order not created");
        }
        if (!ExecutionStatus.SU.equals(inst.getStatus())) {
            log.warn("order_initialization_saga failed orderNo={} sagaStatus={} orderStatus={}", req.getOrderNo(), inst.getStatus(), order.getStatus());
            return order;
        }
        log.info("order_initialization_saga success orderNo={} status={}", req.getOrderNo(), order.getStatus());
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

    @Override
    public Order findByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return null;
        }
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
    }
}
