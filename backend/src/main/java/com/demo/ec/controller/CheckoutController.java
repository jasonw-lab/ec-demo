package com.demo.ec.controller;

import com.demo.ec.model.CartItem;
import com.demo.ec.model.OrderRequest;
import com.demo.ec.model.Product;
import com.demo.ec.paypay.PayPayService;
import com.demo.ec.repo.DemoData;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final PayPayService payPayService;

    public CheckoutController(PayPayService payPayService) {
        this.payPayService = payPayService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody OrderRequest request) {
        // calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : request.items()) {
            Product p = DemoData.products.stream().filter(pr -> pr.id().equals(item.productId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));
            total = total.add(p.price().multiply(BigDecimal.valueOf(item.quantity())));
        }

        // simulate order creation
        String orderId = UUID.randomUUID().toString();
        Map<String, Object> order = new HashMap<>();
        order.put("id", orderId);
        order.put("amount", total);
        order.put("currency", "JPY");
        order.put("status", "PENDING_PAYMENT");
        order.put("customerName", request.customerName());
        order.put("customerEmail", request.customerEmail());
        order.put("items", request.items());
        DemoData.orders.put(orderId, order);

        // Create PayPay payment and get redirect URL
        String redirectUrl = payPayService.createPaymentUrl(orderId, total, order);

        Map<String, Object> resp = new HashMap<>();
        resp.put("orderId", orderId);
        resp.put("amount", total);
        resp.put("paymentUrl", redirectUrl);
        resp.put("status", "PENDING_PAYMENT");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(order);
    }

    @PostMapping("/payments/{id}/simulate-success")
    public ResponseEntity<Map<String, Object>> simulateSuccess(@PathVariable String id) {
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) return ResponseEntity.notFound().build();
        order.put("status", "PAID");
        return ResponseEntity.ok(order);
    }
}
