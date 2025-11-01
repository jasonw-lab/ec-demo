package com.demo.ec.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Placeholder webhook endpoint for PayPay callbacks.
 * Real-world deployments should verify signatures and forward the event to order-service.
 */
@RestController
@RequestMapping("/api/paypay")
public class PayPayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayPayWebhookController.class);

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        log.info("Received PayPay webhook payload={}", payload);
        return ResponseEntity.ok().build();
    }
}
