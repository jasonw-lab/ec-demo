package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.client.AccountTccClient;
import com.example.seata.at.order.client.StorageTccClient;
import com.example.seata.at.order.client.dto.DebitTccRequest;
import com.example.seata.at.order.client.dto.DeductTccRequest;
import com.example.seata.at.order.domain.entity.TccOrder;
import com.example.seata.at.order.domain.mapper.TccOrderMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Order TCC Service Implementation
 */
@Service
public class OrderTccServiceImpl implements OrderTccService {
    private static final Logger log = LoggerFactory.getLogger(OrderTccServiceImpl.class);
    private final TccOrderMapper orderMapper;
    private final StorageTccClient storageTccClient;
    private final AccountTccClient accountTccClient;

    public OrderTccServiceImpl(TccOrderMapper orderMapper, StorageTccClient storageTccClient, AccountTccClient accountTccClient) {
        this.orderMapper = orderMapper;
        this.storageTccClient = storageTccClient;
        this.accountTccClient = accountTccClient;
    }

    @Override
//    @GlobalTransactional(name = "order-create-tcc-tx", rollbackFor = Exception.class)
    public TccOrder tryCreate(OrderDTO req, String orderNo) {
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("=== TCC オーダー作成開始 === orderNo={}, userId={}, productId={}, count={}, amount={}, xid={}",
                req.getOrderNo(), req.getUserId(), req.getProductId(), req.getCount(), req.getAmount(), xid);

        // 冪等性チェック
        TccOrder existingTcc = orderMapper.selectOne(new LambdaQueryWrapper<TccOrder>()
                .eq(TccOrder::getXid, xid));
        if (existingTcc != null) {
            log.info("TCCオーダー既存（冪等性）: xid={}, status={}", xid, existingTcc.getStatus());
//            // 既存のオーダーを返す
//            TccOrder tccOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
//                    .eq(Order::getOrderNo, existingTcc.getOrderNo()));
            return existingTcc;
        }

        // TCCオーダー作成（ステータス: PENDING）
        TccOrder tccOrder = new TccOrder();
        tccOrder.setXid(xid);
        tccOrder.setOrderNo(req.getOrderNo());
        tccOrder.setUserId(req.getUserId());
        tccOrder.setProductId(req.getProductId());
        tccOrder.setCount(req.getCount());
        tccOrder.setAmount(req.getAmount());
        tccOrder.setStatus("PENDING"); // PENDING
        orderMapper.insert(tccOrder);
        log.info("TCCオーダー作成完了: xid={}, orderNo={}", xid, req.getOrderNo());

        // Storage TCC Try 呼び出し
        log.info("--- Storage TCC Try 呼び出し ---");
        DeductTccRequest deductReq = new DeductTccRequest();
        deductReq.setOrderNo(req.getOrderNo());
        deductReq.setProductId(req.getProductId());
        deductReq.setCount(req.getCount());
        
        try {
            log.info("--- Storage TCC Try 呼び出し ---productId= {}, orderNo= {},count= {} ",deductReq.getProductId(),deductReq.getOrderNo(),deductReq.getCount());
            CommonResponse<String> storageRes = storageTccClient.tryDeduct(deductReq);
            if (storageRes == null || !Boolean.TRUE.equals(storageRes.isSuccess())) {
                throw new RuntimeException("Storage TCC try failed: " + (storageRes == null ? "null" : storageRes.getMessage()));
            }
            log.info("Storage TCC Try 成功");
        } catch (Exception e) {
            log.error("Storage TCC Try 失敗: {}", e.getMessage());
            throw new RuntimeException("Storage TCC try failed: " + e.getMessage(), e);
        }

        // Account TCC Try 呼び出し
        log.info("--- Account TCC Try 呼び出し ---");
        DebitTccRequest debitReq = new DebitTccRequest();
        debitReq.setOrderNo(req.getOrderNo());
        debitReq.setOrderId(tccOrder.getId());
        debitReq.setUserId(req.getUserId());
        debitReq.setAmount(req.getAmount());
        
        try {
            CommonResponse<String> accountRes = accountTccClient.tryDebit(debitReq);
            if (accountRes == null || !Boolean.TRUE.equals(accountRes.isSuccess())) {
                throw new RuntimeException("Account TCC try failed: " + (accountRes == null ? "null" : accountRes.getMessage()));
            }
            log.info("Account TCC Try 成功");
        } catch (Exception e) {
            log.error("Account TCC Try 失敗: {}", e.getMessage());
            throw new RuntimeException("Account TCC try failed: " + e.getMessage(), e);
        }

        // 実際のオーダーを作成（ステータス: 1=確定）
//        Order order = new Order();
//        order.setOrderNo(req.getOrderNo());
//        order.setUserId(req.getUserId());
//        order.setProductId(req.getProductId());
//        order.setCount(req.getCount());
//        order.setAmount(req.getAmount());
//        order.setStatus(1); // 確定
//        orderMapper.insert(order);
        
//        // TCCオーダーを成功に更新
        tccOrder.setStatus("CONFIRMED"); // CONFIRMED
        orderMapper.updateById(tccOrder);
        
        log.info("=== TCC オーダー作成完了 === orderNo={}, id={}", tccOrder.getOrderNo(), tccOrder.getId());

        // 全部成功 → TM commit
        // TM が commit 決定 → storage.confirm / account.confirm / order.confirm 実行
        return tccOrder;
    }

    @Override
    public boolean confirm(BusinessActionContext context) {
        String xid = context != null ? context.getXid() : null;
        log.info("===  OrderTccServiceImpl confirm, xid={} ", xid);
        String orderNo = context.getActionContext("orderNo")!=null ? context.getActionContext("orderNo").toString() : null;

        log.info("===  OrderTccServiceImpl confirm, orderNo={} ", orderNo);

        // Prefer xid to locate the TCC order; 'orderId' may be null if it wasn't passed in TRY phase
        TccOrder order = null;
        try {
            if (xid != null) {
                order = orderMapper.selectOne(new LambdaQueryWrapper<TccOrder>().eq(TccOrder::getXid, xid));
            }
            if (order == null) {
                Object idObj = context != null ? context.getActionContext("orderId") : null;
                if (idObj != null) {
                    Long orderId = (idObj instanceof Number) ? ((Number) idObj).longValue() : null;
                    if (orderId != null) {
                        order = orderMapper.selectById(orderId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("confirm: failed to load TCC order, xid={}, err={}", xid, e.getMessage(), e);
        }

        if (order == null) {
            // Idempotent: if the order record is missing, treat as success to avoid endless retries
            log.warn("confirm: TCC order not found by xid={}, treat as idempotent success", xid);
            return true;
        }

        if ("CONFIRMED".equals(order.getStatus())) {
            log.info("confirm: already CONFIRMED for xid={}, idempotent", xid);
            return true;
        }

        order.setStatus("CONFIRMED");
        orderMapper.updateById(order);
        return true;
    }

    @Override
    public boolean cancel(BusinessActionContext context) {
        String xid = context != null ? context.getXid() : null;
        String orderNo = context.getActionContext("orderNo")!=null ? context.getActionContext("orderNo").toString() : null;
        log.info("===  OrderTccServiceImpl cancel, xid={} ", xid);
        log.info("===  OrderTccServiceImpl cancel, orderNo={} ", orderNo);

        TccOrder order = null;
        try {
            if (xid != null) {
                order = orderMapper.selectOne(new LambdaQueryWrapper<TccOrder>().eq(TccOrder::getXid, xid));
            }
            if (order == null) {
                Object idObj = context != null ? context.getActionContext("orderId") : null;
                if (idObj != null) {
                    Long orderId = (idObj instanceof Number) ? ((Number) idObj).longValue() : null;
                    if (orderId != null) {
                        order = orderMapper.selectById(orderId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("cancel: failed to load TCC order, xid={}, err={}", xid, e.getMessage(), e);
        }

        if (order == null) {
            log.warn("cancel: TCC order not found by xid={}, treat as idempotent success", xid);
            return true;
        }

        if ("CANCELLED".equals(order.getStatus())) {
            log.info("cancel: already CANCELLED for xid={}, idempotent", xid);
            return true;
        }

        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
        return true;
    }
}