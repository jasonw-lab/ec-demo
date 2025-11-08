package com.example.seata.at.order.api;

import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.PaymentStatusUpdateRequest;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.service.OrderPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderPaymentController {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentController.class);

    private final OrderPaymentService orderPaymentService;

    public OrderPaymentController(OrderPaymentService orderPaymentService) {
        this.orderPaymentService = orderPaymentService;
    }

    /**
     * Webhookから呼び出される支払いイベント処理エンドポイント
     * 
     * <p>BFFのPayPayWebhookControllerから呼び出され、注文ステータスを更新します。
     * 
     * @param orderNo 注文番号
     * @param request 支払いステータス更新リクエスト
     * @return 更新された注文
     */
    @PostMapping("/{orderNo}/payment/events")
    public CommonResponse<Order> handlePaymentEvent(@PathVariable String orderNo,
                                                    @RequestBody PaymentStatusUpdateRequest request) {
        log.info("[PaymentEvent] Received payment event orderNo={}, status={}, eventId={}", 
                orderNo, request.getStatus(), request.getEventId());
        Order updated = orderPaymentService.handlePaymentStatus(orderNo, request);
        CommonResponse<Order> res = new CommonResponse<>();
        res.setSuccess(true);
        res.setMessage("OK");
        res.setData(updated);
        log.info("[PaymentEvent] Payment event processed successfully orderNo={}, newStatus={}", 
                orderNo, updated.getStatus());
        return res;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[PaymentEvent] invalid request: {}", ex.getMessage());
        CommonResponse<Void> res = new CommonResponse<>();
        res.setSuccess(false);
        res.setMessage(ex.getMessage());
        return ResponseEntity.badRequest().body(res);
    }
}
