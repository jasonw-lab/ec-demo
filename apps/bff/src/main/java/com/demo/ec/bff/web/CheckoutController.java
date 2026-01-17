package com.demo.ec.bff.web;

import com.demo.ec.bff.application.auth.AuthSessionFilter;
import com.demo.ec.bff.application.auth.SessionData;
import com.demo.ec.bff.gateway.client.OrderServiceClient;
import com.demo.ec.bff.gateway.client.StorageServiceClient;
import com.demo.ec.bff.gateway.client.dto.OrderServiceRequest;
import com.demo.ec.bff.gateway.client.dto.OrderSummary;
import com.demo.ec.bff.domain.CartItem;
import com.demo.ec.bff.domain.OrderRequest;
import com.demo.ec.bff.domain.Product;
import com.demo.ec.bff.domain.DemoData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private static final Duration PAYMENT_URL_WAIT = Duration.ofSeconds(5);

    private final OrderServiceClient orderServiceClient;
    private final StorageServiceClient storageServiceClient;

    public CheckoutController(OrderServiceClient orderServiceClient,
                              StorageServiceClient storageServiceClient) {
        this.orderServiceClient = orderServiceClient;
        this.storageServiceClient = storageServiceClient;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody OrderRequest request,
                                                        HttpServletRequest httpRequest) {
        log.info("CheckoutController.checkout START request: customerName={}, customerEmail={}, items={}", request.customerName(), request.customerEmail(), request.items());
        SessionData session = (SessionData) httpRequest.getAttribute(AuthSessionFilter.REQ_ATTR_SESSION);
        if (session == null || session.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "UNAUTHORIZED", "message", "ログインが必要です"));
        }
        // calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : request.items()) {
            Product p = findProduct(item.productId());
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
        resp.put("userId", session.getUserId());
        log.info("CheckoutController.checkout END response: orderId={}, amount={}, status={}", orderId, total, "PENDING_PAYMENT");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/orders/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@Valid @RequestBody OrderRequest request,
                                                        HttpServletRequest httpRequest) {
        log.info("CheckoutController.purchase START customerName={} email={} items={}", request.customerName(), request.customerEmail(), request.items());
        SessionData session = (SessionData) httpRequest.getAttribute(AuthSessionFilter.REQ_ATTR_SESSION);
        if (session == null || session.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "UNAUTHORIZED", "message", "ログインが必要です"));
        }
        if (request.items() == null || request.items().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "NO_ITEMS", "message", "購入する商品が選択されていません"));
        }

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, Integer> counts = new HashMap<>();
        try {
            for (CartItem item : request.items()) {
                Product product = findProduct(item.productId());
                total = total.add(product.price().multiply(BigDecimal.valueOf(item.quantity())));
                counts.merge(product.id(), item.quantity(), Integer::sum);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("CheckoutController.purchase invalid product: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "INVALID_PRODUCT", "message", ex.getMessage()));
        }

        if (counts.size() != 1) {
            log.warn("CheckoutController.purchase multiple products not supported counts={}", counts.keySet());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "MULTI_PRODUCT_UNSUPPORTED", "message", "このデモでは1種類の商品だけ購入できます"));
        }

        Long productId = counts.keySet().iterator().next();
        Integer count = counts.get(productId);
        String orderNo = UUID.randomUUID().toString();

        OrderServiceRequest orderReq = new OrderServiceRequest();
        orderReq.setUserId(session.getUserId());
        orderReq.setProductId(productId);
        orderReq.setCount(count);
        orderReq.setAmount(total);
        orderReq.setOrderNo(orderNo);

        Optional<OrderSummary> created = orderServiceClient.createOrderSaga(orderReq);
        if (created.isEmpty()) {
            log.error("CheckoutController.purchase failed to create order via order-service orderNo={}", orderNo);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("code", "ORDER_SERVICE_ERROR", "message", "注文作成に失敗しました。しばらくしてから再度お試しください。"));
        }

        OrderSummary summary = created.get();
        String resolvedOrderNo = StringUtils.hasText(summary.getOrderNo()) ? summary.getOrderNo() : orderNo;
        BigDecimal resolvedAmount = summary.getAmount() != null ? summary.getAmount() : total;
        String status = mapOrderStatus(summary.getStatus());

        Map<String, Object> resp = new HashMap<>();
        resp.put("orderId", resolvedOrderNo);
        resp.put("amount", resolvedAmount);
        resp.put("status", status);
        if (StringUtils.hasText(summary.getPaymentUrl())) {
            resp.put("paymentUrl", summary.getPaymentUrl());
        }
        if (StringUtils.hasText(summary.getPaymentChannelToken())) {
            resp.put("channelToken", summary.getPaymentChannelToken());
            if (summary.getPaymentChannelExpiresAt() != null) {
                resp.put("channelTokenExpiresAt", summary.getPaymentChannelExpiresAt().toString());
            }
        }

        log.info("CheckoutController.purchase END orderId={} amount={} status={}", resolvedOrderNo, resolvedAmount, status);
        return ResponseEntity.ok(resp);
    }

    /**
     * 支払い詳細情報を取得するエンドポイント
     * 
     * <p>呼び出し元：
     * <ul>
     *   <li>フロントエンド: {@code PaymentDetailView.vue} の {@code startPolling()} 関数から定期的に呼び出される</li>
     *   <li>ポーリング間隔: 3秒ごと（{@code pollIntervalMs = 3000}）</li>
     *   <li>目的: 支払いステータス（PAID/CANCELLED）を検知して画面遷移をトリガー</li>
     * </ul>
     * 
     * <p>注意: 支払い完了後（PAIDステータス）は、フロントエンド側で {@code hasFinalized} フラグにより
     * ポーリングが停止されるため、このエンドポイントは呼ばれなくなります。
     * 
     * @param id 注文ID（orderNo）
     * @return 支払い詳細情報（ステータス、金額、QRコードURL等）
     */
    @GetMapping("/payments/{id}/details")
    public ResponseEntity<Map<String, Object>> getPaymentDetails(@PathVariable String id) {
        log.info("CheckoutController.getPaymentDetails START request: id={}", id);
        Optional<OrderSummary> orderOpt = orderServiceClient.getOrder(id);
        if (orderOpt.isEmpty()) {
            log.warn("CheckoutController.getPaymentDetails order not found for id={}", id);
            return ResponseEntity.notFound().build();
        }

        OrderSummary summary = orderOpt.get();
        Map<String, Object> payload = toOrderPayload(summary);
        String status = (String) payload.get("status");

        // 支払い完了済みの場合はログに記録（通常は呼ばれないはず）
        if ("PAID".equals(status) || "PAYMENT_FAILED".equals(status)) {
            log.debug("CheckoutController.getPaymentDetails called for finalized order orderId={}, status={}", id, status);
        }

        log.info("CheckoutController.getPaymentDetails END orderId={} status={}", id, status);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        log.info("CheckoutController.getOrder START request: id={}", id);
        Optional<OrderSummary> orderOpt = orderServiceClient.getOrder(id);
        if (orderOpt.isEmpty()) {
            log.warn("CheckoutController.getOrder not found id={}", id);
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> payload = toOrderPayload(orderOpt.get());
        log.info("CheckoutController.getOrder END orderId={} status={}", id, payload.get("status"));
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/payments/{id}/qrcode")
    public ResponseEntity<Map<String, String>> getQRCode(@PathVariable String id, @RequestParam(value = "amount", required = false) BigDecimal amount) {
        log.info("CheckoutController.getQRCode START request: id={}, amountParam={}", id, amount);
        Optional<OrderSummary> orderOpt = orderServiceClient.getOrder(id);
        if (orderOpt.isEmpty()) {
            log.warn("CheckoutController.getQRCode order not found id={}", id);
            return ResponseEntity.notFound().build();
        }

        OrderSummary orderSummary = orderOpt.get();
        String currentStatus = orderSummary.getStatus();
        
        // 注文が既に終了状態（失敗または完了）の場合は早期リターン
        if (currentStatus != null && (currentStatus.equalsIgnoreCase("CANCELLED")
                || currentStatus.equalsIgnoreCase("PAYMENT_FAILED") 
                || currentStatus.equalsIgnoreCase("PAID"))) {
            log.warn("CheckoutController.getQRCode order already in terminal state orderId={} status={}", id, currentStatus);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", mapOrderStatus(currentStatus), 
                                 "message", "注文は既に処理済みです"));
        }
        
        BigDecimal total = amount != null
                ? amount
                : orderSummary.getAmount() != null ? orderSummary.getAmount() : BigDecimal.ZERO;

        Optional<OrderSummary> ready = waitForPaymentInfo(id, orderSummary);
        if (ready.isEmpty() || !StringUtils.hasText(ready.get().getPaymentUrl())) {
            log.info("CheckoutController.getQRCode paymentUrl not ready orderId={}", id);
            String waitingStatus = mapOrderStatus(ready.map(OrderSummary::getStatus).orElse(orderSummary.getStatus()));
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", waitingStatus));
        }

        String url = ready.get().getPaymentUrl();
        try {
            String base64 = generateQrBase64(url);
            Map<String, String> resp = new HashMap<>();
            resp.put("base64Image", base64);
            resp.put("paymentUrl", url);
            resp.put("amount", total.toPlainString());
            log.info("CheckoutController.getQRCode END response: base64 length={} for id={}", base64.length(), id);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("CheckoutController.getQRCode error for id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Optional<OrderSummary> waitForPaymentInfo(String orderNo, OrderSummary seed) {
        long deadline = System.nanoTime() + PAYMENT_URL_WAIT.toNanos();
        Optional<OrderSummary> current = Optional.ofNullable(seed);
        while (System.nanoTime() < deadline) {
            if (current.isPresent() && StringUtils.hasText(current.get().getPaymentUrl())) {
                return current;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return current;
            }
            current = orderServiceClient.getOrder(orderNo);
            if (current.isEmpty()) {
                return Optional.empty();
            }
        }
        return current;
    }

    private String generateQrBase64(String url) throws Exception {
        QRCodeWriter qrWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = qrWriter.encode(url, BarcodeFormat.QR_CODE, 256, 256, hints);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private Map<String, Object> toOrderPayload(OrderSummary summary) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", summary.getOrderNo());
        if (summary.getAmount() != null) {
            payload.put("amount", summary.getAmount());
        }
        payload.put("status", mapOrderStatus(summary.getStatus()));
        if (StringUtils.hasText(summary.getPaymentStatus())) {
            payload.put("paypayStatus", summary.getPaymentStatus());
        }
        if (summary.getPaymentExpiresAt() != null) {
            payload.put("paymentExpiresAt", summary.getPaymentExpiresAt().toString());
        }
        if (StringUtils.hasText(summary.getPaymentChannelToken())) {
            payload.put("channelToken", summary.getPaymentChannelToken());
        }
        if (summary.getPaymentChannelExpiresAt() != null) {
            payload.put("channelTokenExpiresAt", summary.getPaymentChannelExpiresAt().toString());
        }
        if (summary.getPaymentRequestedAt() != null) {
            payload.put("paymentRequestedAt", summary.getPaymentRequestedAt().toString());
        }
        if (summary.getPaymentCompletedAt() != null) {
            payload.put("paymentCompletedAt", summary.getPaymentCompletedAt().toString());
        }
        if (StringUtils.hasText(summary.getPaymentUrl())) {
            payload.put("paymentUrl", summary.getPaymentUrl());
        }
        if (StringUtils.hasText(summary.getFailCode()) || StringUtils.hasText(summary.getFailMessage())) {
            Map<String, Object> error = new HashMap<>();
            if (StringUtils.hasText(summary.getFailCode())) {
                error.put("code", summary.getFailCode());
            }
            if (StringUtils.hasText(summary.getFailMessage())) {
                error.put("message", summary.getFailMessage());
            }
            payload.put("paypayError", error);
        }
        return payload;
    }

    private String mapOrderStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return "PENDING_PAYMENT";
        }
        return switch (rawStatus.toUpperCase()) {
            case "PAID" -> "PAID";
            case "CANCELLED" -> "PAYMENT_FAILED";
            case "PAYMENT_FAILED" -> "PAYMENT_FAILED";
            case "AUTHORIZED" -> "AUTHORIZED";
            default -> "PENDING_PAYMENT";
        };
    }

    private Product findProduct(Long productId) {
        StorageServiceClient.StockLookupResult stockResult = storageServiceClient.getStock(productId);
        if (stockResult.reachable() && stockResult.stock().isEmpty()) {
            throw new IllegalArgumentException("商品が存在しません: " + productId);
        }
        return storageServiceClient.getProduct(productId)
                .orElseGet(() -> DemoData.products.stream()
                        .filter(p -> p.id().equals(productId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("商品が存在しません: " + productId)));
    }
}
