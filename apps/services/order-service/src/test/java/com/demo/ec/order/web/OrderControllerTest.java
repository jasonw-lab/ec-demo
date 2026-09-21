package com.demo.ec.order.web;

import com.demo.ec.order.application.OrderATService;
import com.demo.ec.order.application.OrderSagaService;
import com.demo.ec.order.application.OrderTccService;
import com.demo.ec.order.domain.Order;
import com.demo.ec.order.domain.TccOrder;
import com.demo.ec.order.web.dto.OrderDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderATService orderATService;

    @MockBean
    private OrderTccService orderTccService;

    @MockBean
    private OrderSagaService orderSagaService;

    @Test
    void createOrderSaga_shouldReturnSuccess() throws Exception {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-001");

        Order order = new Order();
        order.setOrderNo("ORDER-001");
        order.setStatus("CREATED");

        when(orderSagaService.startOrderCreateSaga(any(OrderDTO.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders/saga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_no").value("ORDER-001"));
    }

    @Test
    void createOrderSaga_shouldReturnFailureWhenServiceReturnsNull() throws Exception {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-002");

        when(orderSagaService.startOrderCreateSaga(any(OrderDTO.class))).thenReturn(null);

        mockMvc.perform(post("/api/orders/saga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("FAILED"));
    }

    @Test
    void createOrderSaga_shouldReturnBadRequestWhenInvalid() throws Exception {
        OrderDTO req = new OrderDTO();
        // Missing required fields

        mockMvc.perform(post("/api/orders/saga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getByOrderNo_shouldReturnOrder() throws Exception {
        Order order = new Order();
        order.setOrderNo("ORDER-003");
        order.setStatus("PAID");

        when(orderSagaService.findByOrderNo("ORDER-003")).thenReturn(order);

        mockMvc.perform(get("/api/orders/ORDER-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_no").value("ORDER-003"));
    }

    @Test
    void getByOrderNo_shouldReturnNotFound() throws Exception {
        when(orderSagaService.findByOrderNo("ORDER-999")).thenReturn(null);

        mockMvc.perform(get("/api/orders/ORDER-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NOT_FOUND"));
    }

    @Test
    void createOrder_shouldGenerateOrderNoWhenMissing() throws Exception {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));

        Order order = new Order();
        order.setOrderNo("GENERATED-001");
        order.setStatus("CREATED");

        when(orderATService.placeOrder(any(OrderDTO.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_no").value("GENERATED-001"));
    }

    @Test
    void createOrderTcc_shouldReturnSuccess() throws Exception {
        OrderDTO req = new OrderDTO();
        req.setUserId(1L);
        req.setProductId(1L);
        req.setCount(1);
        req.setAmount(new BigDecimal("10.00"));
        req.setOrderNo("ORDER-004");

        TccOrder order = new TccOrder();
        order.setOrderNo("ORDER-004");

        when(orderTccService.tryCreate(any(OrderDTO.class), any(String.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders/tcc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_no").value("ORDER-004"));
    }
}
