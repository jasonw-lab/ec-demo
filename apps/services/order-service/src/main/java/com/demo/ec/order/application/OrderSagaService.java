package com.demo.ec.order.application;

import com.demo.ec.order.web.dto.OrderDTO;
import com.demo.ec.order.domain.Order;

public interface OrderSagaService {
    Order createOrderSaga(OrderDTO req);

    Order startOrderCreateSaga(OrderDTO req);

    boolean startSampleReduceInventoryAndBalance(OrderDTO req);

    Order findByOrderNo(String orderNo);
}

