package com.demo.ec.controller;

import com.demo.ec.model.CartItem;
import com.demo.ec.model.OrderRequest;
import com.demo.ec.model.Product;
import com.demo.ec.repo.DemoData;
import com.demo.ec.pay.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ZXing for QR code generation
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final PaymentService payPayService;

    public CheckoutController(PaymentService payPayService) {
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

    @GetMapping("/payments/{id}/details")
    public ResponseEntity<Map<String, Object>> getPaymentDetails(@PathVariable String id) {
        log.info("CheckoutController.getPaymentDetails START request: id={}", id);
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) {
            log.warn("CheckoutController.getPaymentDetails order not found: id={}", id);
            return ResponseEntity.notFound().build();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paypay = payPayService.getPaymentDetails(id);
            if (paypay != null) {
                // Check for PayPay API errors first
                Object error = paypay.get("error");
                if (error instanceof Map) {
                    Map<?, ?> errorMap = (Map<?, ?>) error;
                    String errorCode = String.valueOf(errorMap.get("code"));
                    String errorMessage = String.valueOf(errorMap.get("message"));
                    log.warn("PayPay API error for order {}: {} - {}", id, errorCode, errorMessage);
                    order.put("paypayError", Map.of("code", errorCode, "message", errorMessage));
                    
                    // Handle specific error codes that should fail the payment
                    if ("00000900".equals(errorCode) || "UNKNOWN".equals(errorCode)) {
                        order.put("status", "PAYMENT_FAILED");
                        order.put("paypayStatus", "ERROR");
                    }
                } else {
                    // Extract PayPay status from response
                    String paypayStatus = null;
                    Object data = paypay.get("data");
                    if (data instanceof Map<?,?>) {
                        Map<?,?> dataMap = (Map<?,?>) data;
                        Object s = dataMap.get("status");
                        if (s == null) s = dataMap.get("paymentStatus");
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
            }
        } catch (Exception e) {
            // Do not break the flow if PayPay call fails; just return current stored order.
            log.error("CheckoutController.getPaymentDetails error when fetching PayPay details for id={}: {}", id, e.getMessage(), e);
            order.put("paypayError", Map.of("code", "SYSTEM_ERROR", "message", e.getMessage()));
        }

        log.info("CheckoutController.getPaymentDetails END response: order={}", order);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        log.info("CheckoutController.getOrder START request: id={}", id);
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) return ResponseEntity.notFound().build();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paypay = payPayService.getPaymentDetails(id);
            if (paypay != null) {
                // Check for PayPay API errors first
                Object error = paypay.get("error");
                if (error instanceof Map) {
                    Map<?, ?> errorMap = (Map<?, ?>) error;
                    String errorCode = String.valueOf(errorMap.get("code"));
                    String errorMessage = String.valueOf(errorMap.get("message"));
                    log.warn("PayPay API error for order {}: {} - {}", id, errorCode, errorMessage);
                    order.put("paypayError", Map.of("code", errorCode, "message", errorMessage));
                    
                    // Handle specific error codes that should fail the payment
                    if ("00000900".equals(errorCode) || "UNKNOWN".equals(errorCode)) {
                        order.put("status", "PAYMENT_FAILED");
                        order.put("paypayStatus", "ERROR");
                    }
                } else {
                    // Extract PayPay status from response
                    String paypayStatus = null;
                    Object data = paypay.get("data");
                    if (data instanceof Map<?,?>) {
                        Map<?,?> dataMap = (Map<?,?>) data;
                        Object s = dataMap.get("status");
                        if (s == null) s = dataMap.get("paymentStatus");
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
            }
        } catch (Exception e) {
            // Do not break the flow if PayPay call fails; just return current stored order.
            log.error("CheckoutController.getOrder error when fetching PayPay details for id={}: {}", id, e.getMessage(), e);
            order.put("paypayError", Map.of("code", "SYSTEM_ERROR", "message", e.getMessage()));
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
    public ResponseEntity<Map<String, String>> getQRCode(@PathVariable String id, @RequestParam(value = "amount", required = false) BigDecimal amount) {
        log.info("CheckoutController.getQRCode START request: id={}, amountParam={}", id, amount);
        Map<String, Object> order = (Map<String, Object>) DemoData.orders.get(id);
        if (order == null) return ResponseEntity.notFound().build();
        BigDecimal total;
        if (amount != null) {
            total = amount;
        } else {
            Object amt = order.get("amount");
            if (amt instanceof BigDecimal) {
                total = (BigDecimal) amt;
            } else if (amt != null) {
                try { total = new BigDecimal(String.valueOf(amt)); } catch (Exception e) { total = BigDecimal.ZERO; }
            } else {
                total = BigDecimal.ZERO;
            }
        }
        try {
            String url = payPayService.createPaymentUrl(id, total, order);
            if (url == null || url.isBlank()) {
                log.error("CheckoutController.getQRCode error: empty paymentUrl for id={}", id);
                return ResponseEntity.internalServerError().build();
            }
            int size = 256;
            QRCodeWriter qrWriter = new QRCodeWriter();
            java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = qrWriter.encode(url, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            Map<String, String> resp = new HashMap<>();
            resp.put("base64Image", base64);
            log.info("CheckoutController.getQRCode END response: base64 length={} for id={}", base64.length(), id);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("CheckoutController.getQRCode error for id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
