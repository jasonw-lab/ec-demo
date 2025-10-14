package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OrderATServiceImpl implements OrderATService {
    private static final Logger log = LoggerFactory.getLogger(OrderATServiceImpl.class);

    private final OrderMapper orderMapper;
    private final RestTemplate restTemplate;

    private static final String STORAGE_BASE_URL = "http://localhost:8082";
    private static final String ACCOUNT_BASE_URL = "http://localhost:8083";


    @Override
    @GlobalTransactional(name = "order-create-tx", rollbackFor = Exception.class)
    public Order placeOrder(OrderDTO req) {
        String xid = null;
        try {
            xid = io.seata.core.context.RootContext.getXID();
        } catch (Throwable ignore) {}
        log.info("Start placeOrder: orderNo={}, userId={}, productId={}, count={}, amount={}, xid={}",
                req.getOrderNo(), req.getUserId(), req.getProductId(), req.getCount(), req.getAmount(), xid);
        // 0) 冪等性: 注文番号で既存注文を確認（完了済ならそのまま返す）
        Order existing = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, req.getOrderNo()));
        if (existing != null) {
            log.info("Idempotent: order already exists. orderNo={}, id={}, status={}", existing.getOrderNo(), existing.getId(), existing.getStatus());
            return existing; // 既に作成済み（成功扱い）
        }

        // 1) 注文レコード作成（グローバルトランザクション内のローカル操作）
        Order order = new Order();
        order.setOrderNo(req.getOrderNo());
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setCount(req.getCount());
        order.setAmount(req.getAmount());
        order.setStatus(0);
        try {
            log.info("1) 注文レコード作成: inserting new order. orderNo={}, userId={}, productId={}, count={}, amount={}",
                    req.getOrderNo(), req.getUserId(), req.getProductId(), req.getCount(), req.getAmount());
            orderMapper.insert(order);
            log.info("1) 注文レコード作成: inserted. id={}, orderNo={}", order.getId(), order.getOrderNo());
        } catch (DuplicateKeyException dup) {
            // 他リトライ/並行リクエストで同じ orderNo が挿入された場合は既存を返す
            log.warn("1) 注文レコード作成: duplicate key on orderNo={}, trying to load existing record", req.getOrderNo());
            Order dupOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, req.getOrderNo()));
            if (dupOrder != null) {
                log.info("1) 注文レコード作成: duplicate resolved by existing record. id={}, status={}", dupOrder.getId(), dupOrder.getStatus());
                return dupOrder;
            }
            throw dup;
        }

        // 2) 在庫引当（下位サービス呼び出し）
        DeductRequest deductRequest = new DeductRequest();
        deductRequest.setProductId(req.getProductId());
        deductRequest.setCount(req.getCount());
        CommonResponse<?> storageRes;
        log.info("2) 在庫引当: calling storage-service deduct. productId={}, count={}", req.getProductId(), req.getCount());
        try {
            storageRes = restTemplate.postForObject(STORAGE_BASE_URL + "/api/storage/deduct", deductRequest, CommonResponse.class);
        } catch (RestClientException e) {
            log.error("2) 在庫引当: storage service error: {}", e.getMessage(), e);
            throw new RuntimeException("Storage service error: " + e.getMessage(), e);
        }
        if (storageRes == null || !Boolean.TRUE.equals(storageRes.isSuccess())) {
            log.warn("2) 在庫引当: failed. success={}, message={}", storageRes == null ? null : storageRes.isSuccess(), storageRes == null ? "null" : storageRes.getMessage());
            throw new RuntimeException("Storage deduct failed: " + (storageRes == null ? "null" : storageRes.getMessage()));
        }
        log.info("2) 在庫引当: success. success={}, message={}", storageRes.isSuccess(), storageRes.getMessage());

        // 3) 残高/決済の引落（下位サービス呼び出し）
        DebitRequest debitRequest = new DebitRequest();
        debitRequest.setUserId(req.getUserId());
        debitRequest.setAmount(req.getAmount());
        CommonResponse<?> accountRes;
        log.info("3) 残高/決済の引落: calling account-service debit. userId={}, amount={}", req.getUserId(), req.getAmount());
        try {
            accountRes = restTemplate.postForObject(ACCOUNT_BASE_URL + "/api/account/debit", debitRequest, CommonResponse.class);
        } catch (RestClientException e) {
            log.error("3) 残高/決済の引落: account service error: {}", e.getMessage(), e);
            throw new RuntimeException("Account service error: " + e.getMessage(), e);
        }
        if (accountRes == null || !Boolean.TRUE.equals(accountRes.isSuccess())) {
            log.warn("3) 残高/決済の引落: failed. success={}, message={}", accountRes == null ? null : accountRes.isSuccess(), accountRes == null ? "null" : accountRes.getMessage());
            throw new RuntimeException("Account debit failed: " + (accountRes == null ? "null" : accountRes.getMessage()));
        }
        log.info("3) 残高/決済の引落: success. success={}, message={}", accountRes.isSuccess(), accountRes.getMessage());

        // 4) 注文確定（主キーで1行更新）
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getId, order.getId()).set(Order::getStatus, 1);
        log.info("4) 注文確定: updating status to CONFIRMED. id={}, orderNo={}", order.getId(), order.getOrderNo());
        orderMapper.update(null, uw);
        order.setStatus(1);
        log.info("4) 注文確定: updated. id={}, status={}", order.getId(), order.getStatus());
        log.info("Order placed successfully. orderNo={}, id={}", order.getOrderNo(), order.getId());
        return order;
    }

    // DTOs for cross-service calls (local copy)
    public static class DeductRequest {
        private Long productId;
        private Integer count;
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    public static class DebitRequest {
        private Long userId;
        private java.math.BigDecimal amount;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    }
}
