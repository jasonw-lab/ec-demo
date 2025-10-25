package com.example.seata.at.order.api;

import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.TccOrder;
import com.example.seata.at.order.domain.entity.OrderStatus;
import com.example.seata.at.order.service.OrderATService;
import com.example.seata.at.order.service.OrderTccService;
import com.example.seata.at.order.service.OrderSagaService;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderATService orderATService;
    private final OrderTccService orderTccService;
    private final OrderSagaService orderSagaService;

    public OrderController(OrderATService orderATService, OrderTccService orderTccService, OrderSagaService orderSagaService) {
        this.orderATService = orderATService;
        this.orderTccService = orderTccService;
        this.orderSagaService = orderSagaService;
    }

    @PostMapping
    public CommonResponse<Order> create(@Valid @RequestBody OrderDTO req) {
        if (req.getOrderNo() == null || req.getOrderNo().trim().isEmpty()) {
            // Generate orderNo on server side if client didn't provide one
            req.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        Order order = orderATService.placeOrder(req);
        return new CommonResponse<Order>() {{
            setSuccess(true);
            setMessage("OK");
            setData(order);
        }};
    }

    @PostMapping("/tcc")
    @GlobalTransactional
    public CommonResponse<TccOrder> createOrderTcc(@Valid @RequestBody OrderDTO orderDTO) {
        if (orderDTO.getOrderNo() == null || orderDTO.getOrderNo().trim().isEmpty()) {
            orderDTO.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        TccOrder order =  orderTccService.tryCreate(orderDTO,orderDTO.getOrderNo());
        return new CommonResponse<TccOrder>() {{
            setSuccess(true);
            setMessage("OK");
            setData(order);
        }};
    }

    @PostMapping("/saga")
    @GlobalTransactional
    public CommonResponse<Order> createOrderSaga(@Valid @RequestBody OrderDTO orderDTO) {
        if (orderDTO.getOrderNo() == null || orderDTO.getOrderNo().trim().isEmpty()) {
            orderDTO.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        Order result = orderSagaService.startOrderCreateSaga(orderDTO);
        boolean finalSuccess = result != null && OrderStatus.PAID.name().equalsIgnoreCase(result.getStatus());
        CommonResponse<Order> response = new CommonResponse<>();
        response.setSuccess(finalSuccess);
        String message = finalSuccess ? "OK" : (result != null && result.getFailMessage() != null
                ? result.getFailMessage() : "FAILED");
        response.setMessage(message);
        response.setData(result);
        return response;
    }

    @PostMapping("/saga/sample")
    @GlobalTransactional
    public CommonResponse<Void> runSagaSample(@Valid @RequestBody OrderDTO orderDTO) {
        if (orderDTO.getOrderNo() == null || orderDTO.getOrderNo().trim().isEmpty()) {
            orderDTO.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        boolean success = orderSagaService.startSampleReduceInventoryAndBalance(orderDTO);
        CommonResponse<Void> res = new CommonResponse<>();
        res.setSuccess(success);
        res.setMessage(success ? "OK" : "FAILED");
        return res;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        CommonResponse<Void> res = new CommonResponse<>();
        res.setSuccess(false);
        res.setMessage(msg);
        return ResponseEntity.badRequest().body(res);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CommonResponse<Void>> handleRuntime(RuntimeException ex) {
        CommonResponse<Void> res = new CommonResponse<>();
        res.setSuccess(false);
        res.setMessage(ex.getMessage());
        return ResponseEntity.badRequest().body(res);
    }
}
