package com.example.seata.at.order.service;

import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.Order;

/**
 * Order AT Service Interface
 */
public interface OrderATService {
    
    /**
     * 注文を作成する（AT モード）
     * @param req 注文作成リクエスト
     * @return 作成された注文
     */
    Order placeOrder(OrderDTO req);
}
