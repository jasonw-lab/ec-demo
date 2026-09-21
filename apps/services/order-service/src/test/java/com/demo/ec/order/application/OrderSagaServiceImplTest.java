package com.demo.ec.order.application;

import com.demo.ec.order.domain.Order;
import com.demo.ec.order.gateway.OrderMapper;
import com.demo.ec.order.web.dto.OrderDTO;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private StateMachineEngine stateMachineEngine;

    @InjectMocks
    private OrderSagaServiceImpl orderSagaService;

    @Test
    void startOrderCreateSaga_shouldReturnOrderWhenSuccessful() {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-SAGA-001");

        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.SU);
        when(stateMachineEngine.startWithBusinessKey(
                eq("order_initialization_saga"),
                eq(null),
                eq("ORDER-SAGA-001"),
                anyMap()
        )).thenReturn(instance);

        Order order = new Order();
        order.setOrderNo("ORDER-SAGA-001");
        order.setStatus("CREATED");
        when(orderMapper.selectOne(any())).thenReturn(order);

        Order result = orderSagaService.startOrderCreateSaga(req);

        assertNotNull(result);
        assertEquals("ORDER-SAGA-001", result.getOrderNo());
    }

    @Test
    void startOrderCreateSaga_shouldThrowWhenOrderNotFound() {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-SAGA-002");

        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.SU);
        when(stateMachineEngine.startWithBusinessKey(
                eq("order_initialization_saga"),
                eq(null),
                eq("ORDER-SAGA-002"),
                anyMap()
        )).thenReturn(instance);

        when(orderMapper.selectOne(any())).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderSagaService.startOrderCreateSaga(req));
        assertTrue(ex.getMessage().contains("order_initialization_saga failed"));
    }

    @Test
    void startOrderCreateSaga_shouldReturnOrderEvenWhenSagaFails() {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-SAGA-003");

        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.FA);
        when(stateMachineEngine.startWithBusinessKey(
                eq("order_initialization_saga"),
                eq(null),
                eq("ORDER-SAGA-003"),
                anyMap()
        )).thenReturn(instance);

        Order order = new Order();
        order.setOrderNo("ORDER-SAGA-003");
        order.setStatus("FAILED");
        when(orderMapper.selectOne(any())).thenReturn(order);

        Order result = orderSagaService.startOrderCreateSaga(req);

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    void startSampleReduceInventoryAndBalance_shouldReturnTrueWhenSuccessful() {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-SAMPLE-001");

        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.SU);
        when(stateMachineEngine.startWithBusinessKey(
                eq("reduceInventoryAndBalance"),
                eq(null),
                eq("ORDER-SAMPLE-001"),
                anyMap()
        )).thenReturn(instance);

        assertTrue(orderSagaService.startSampleReduceInventoryAndBalance(req));
    }

    @Test
    void startSampleReduceInventoryAndBalance_shouldReturnFalseWhenFailed() {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-SAMPLE-002");

        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.FA);
        when(stateMachineEngine.startWithBusinessKey(
                eq("reduceInventoryAndBalance"),
                eq(null),
                eq("ORDER-SAMPLE-002"),
                anyMap()
        )).thenReturn(instance);

        assertFalse(orderSagaService.startSampleReduceInventoryAndBalance(req));
    }

    @Test
    void findByOrderNo_shouldReturnOrder() {
        Order order = new Order();
        order.setOrderNo("ORDER-FIND-001");
        when(orderMapper.selectOne(any())).thenReturn(order);

        Order result = orderSagaService.findByOrderNo("ORDER-FIND-001");

        assertNotNull(result);
        assertEquals("ORDER-FIND-001", result.getOrderNo());
    }

    @Test
    void findByOrderNo_shouldReturnNullForBlankInput() {
        assertNull(orderSagaService.findByOrderNo(""));
        assertNull(orderSagaService.findByOrderNo(null));
    }
}
