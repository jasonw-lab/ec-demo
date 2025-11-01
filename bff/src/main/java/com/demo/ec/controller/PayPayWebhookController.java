package com.demo.ec.controller;

import com.demo.ec.repo.DemoData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/paypay")
public class PayPayWebhookController {

    /**
     * Webhook to receive PayPay payment status updates.
     * For demo, we accept any payload and mark order as PAID if status is SUCCESS.
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String,Object> payload) {
        // In real world, verify signature header to ensure authenticity.
        Object orderId = payload.get("orderId");
        Object status = payload.get("status");
        if (orderId != null && "SUCCESS".equalsIgnoreCase(String.valueOf(status))) {
            Map<String,Object> order = (Map<String, Object>) DemoData.orders.get(String.valueOf(orderId));
            if (order != null) {
                order.put("status", "PAID");
                return ResponseEntity.ok(order);
            }
        }
        return ResponseEntity.ok().build();
    }
}
