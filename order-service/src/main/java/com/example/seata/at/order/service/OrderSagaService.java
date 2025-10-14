package com.example.seata.at.order.service;

import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.Order;

public interface OrderSagaService {
    Order createOrderSaga(OrderDTO req);

    Order startOrderCreateSaga(OrderDTO req);

    boolean startSampleReduceInventoryAndBalance(OrderDTO req);
}


