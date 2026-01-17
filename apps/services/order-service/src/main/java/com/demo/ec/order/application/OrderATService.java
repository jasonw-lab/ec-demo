package com.demo.ec.order.application;

import com.demo.ec.order.web.dto.OrderDTO;
import com.demo.ec.order.domain.Order;

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
