package com.demo.ec.order.application;

import com.demo.ec.order.web.dto.OrderDTO;
import com.demo.ec.order.domain.TccOrder;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Order TCC Service Interface
 */
//public interface OrderTccService {
//
//    /**
//     * TCC モードで注文を作成する
//     * @param req 注文作成リクエスト
//     * @return 作成された注文
//     */
//    Order tryCreate(OrderCreateRequest req);
//}
@LocalTCC
public interface OrderTccService {
    @TwoPhaseBusinessAction(name = "OrderTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
//    TccOrder tryCreate(OrderDTO orderDTO, @BusinessActionContextParameter(paramName = "orderId") Long orderId);
    TccOrder tryCreate(OrderDTO orderDTO, @BusinessActionContextParameter(paramName = "orderNo") String orderNo);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
