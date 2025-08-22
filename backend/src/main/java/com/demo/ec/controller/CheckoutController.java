package com.demo.ec.controller;

import com.demo.ec.model.CartItem;
import com.demo.ec.model.OrderRequest;
import com.demo.ec.model.Product;
import com.demo.ec.paypay.PayPayService;
import com.demo.ec.repo.DemoData;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final PayPayService payPayService;

    public CheckoutController(PayPayService payPayService) {
        this.payPayService = payPayService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody OrderRequest request) {
        log.info("CheckoutController.checkout START request: customerName={}, customerEmail={}, items={}", request.customerName(), request.customerEmail(), request.items());
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

        Map<String, Object> resp = new HashMap<>();
        resp.put("orderId", orderId);
        resp.put("amount", total);
        resp.put("status", "PENDING_PAYMENT");
        log.info("CheckoutController.checkout END response: orderId={}, amount={}, status={}", orderId, total, "PENDING_PAYMENT");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        log.info("CheckoutController.getOrder START request: id={}", id);
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) return ResponseEntity.notFound().build();

        try {
            Map paypay = payPayService.getPaymentDetails(id);
            if (paypay != null) {
                // Extract PayPay status from response
                String paypayStatus = null;
                Object data = paypay.get("data");
                if (data instanceof Map<?,?> dm) {
                    Object s = ((Map<?,?>) data).get("status");
                    if (s == null) s = ((Map<?,?>) data).get("paymentStatus");
                    if (s != null) paypayStatus = String.valueOf(s);
                }
                if (paypayStatus == null) {
                    Object s = paypay.get("status");
                    if (s != null) paypayStatus = String.valueOf(s);
                }

                if (paypayStatus != null) {
                    order.put("paypayStatus", paypayStatus);
                    String localStatus;
                    switch (paypayStatus.toUpperCase()) {
                        case "COMPLETED":
                        case "SUCCESS":
                            localStatus = "PAID";
                            break;
                        case "AUTHORIZED":
                            localStatus = "AUTHORIZED";
                            break;
                        case "CREATED":
                        case "PENDING":
                        case "ACTIVE":
                            localStatus = "PENDING_PAYMENT";
                            break;
                        case "CANCELED":
                        case "CANCELLED":
                        case "FAILED":
                        case "EXPIRED":
                            localStatus = "PAYMENT_FAILED";
                            break;
                        default:
                            localStatus = String.valueOf(order.getOrDefault("status", "PENDING_PAYMENT"));
                    }
                    order.put("status", localStatus);
                }
            }
        } catch (Exception e) {
            // Do not break the flow if PayPay call fails; just return current stored order.
            log.error("CheckoutController.getOrder error when fetching PayPay details for id={}: {}", id, e.getMessage(), e);
        }

        log.info("CheckoutController.getOrder END response: order={}", order);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/payments/{id}/simulate-success")
    public ResponseEntity<Map<String, Object>> simulateSuccess(@PathVariable String id) {
        log.info("CheckoutController.simulateSuccess START request: id={}", id);
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) return ResponseEntity.notFound().build();
        order.put("status", "PAID");
        log.info("CheckoutController.simulateSuccess END response: order={}", order);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/payments/{id}/qrcode")
    public ResponseEntity<Map<String, Object>> getQRCode(@PathVariable String id) {
        log.info("CheckoutController.getQRCode START request: id={}", id);
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) return ResponseEntity.notFound().build();
        Object amt = order.get("amount");
        BigDecimal total;
        if (amt instanceof BigDecimal) {
            total = (BigDecimal) amt;
        } else if (amt != null) {
            try { total = new BigDecimal(String.valueOf(amt)); } catch (Exception e) { total = BigDecimal.ZERO; }
        } else {
            total = BigDecimal.ZERO;
        }
        String url = payPayService.createPaymentUrl(id, total, order);
        // store on order for reference
        order.put("paymentUrl", url);
        Map<String, Object> resp = new HashMap<>();
        resp.put("orderId", id);
        resp.put("paymentUrl", url);
        log.info("CheckoutController.getQRCode END response: orderId={}, paymentUrl={}", id, url);
        return ResponseEntity.ok(resp);
    }
}
