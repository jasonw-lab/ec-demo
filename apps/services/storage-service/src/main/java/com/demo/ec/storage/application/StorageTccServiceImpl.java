package com.demo.ec.storage.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.ec.storage.domain.TccStorage;
import com.demo.ec.storage.gateway.TccStorageMapper;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Storage の TCC サービス実装
 */
@Service
public class StorageTccServiceImpl implements StorageTccService {
    private static final Logger log = LoggerFactory.getLogger(StorageTccServiceImpl.class);

    private final TccStorageMapper tccStorageMapper;

    public StorageTccServiceImpl(TccStorageMapper tccStorageMapper) {
        this.tccStorageMapper = tccStorageMapper;
    }

    /**
     * 在庫チェック
     */
    @Override
    public boolean checkStock(Long productId, Integer count) {
        TccStorage tccStorage = tccStorageMapper.selectOne(
            new LambdaQueryWrapper<TccStorage>().eq(TccStorage::getProductId, productId));
        return tccStorage != null && tccStorage.getResidue() >= count;
    }

    /**
     * TCC Try: 在庫を凍結
     */
    @Override
    @Transactional
    public boolean tryDeduct(@BusinessActionContextParameter(paramName = "productId") Long productId,
                             @BusinessActionContextParameter(paramName = "count") Integer count) {
        String xid = RootContext.getXID();
        log.info("=== TCC TRY (Storage) === xid={}, productId={}, count={}", xid, productId, count);
        
        // 冪等性チェック
        TccStorage existing = tccStorageMapper.selectOne(
            new LambdaQueryWrapper<TccStorage>().eq(TccStorage::getXid, xid));
        if (existing != null) {
            log.info("TRY already executed (idempotent): xid={}", xid);
            return true;
        }
        
        // 在庫チェック
        if (!checkStock(productId, count)) {
            log.warn("TRY failed: 在庫不足 productId={}, count={}", productId, count);
            throw new RuntimeException("在庫不足");
        }
        
        // 在庫を凍結
        TccStorage existingTccStorage = tccStorageMapper.selectOne(
            new LambdaQueryWrapper<TccStorage>().eq(TccStorage::getProductId, productId));
        
        if (existingTccStorage == null) {
            // 新しいTCCレコードを作成
            TccStorage tccStorage = new TccStorage();
            tccStorage.setXid(xid);
            tccStorage.setProductId(productId);
            tccStorage.setTotal(0); // 初期値
            tccStorage.setUsed(0); // 初期値
            tccStorage.setResidue(0); // 初期値
            tccStorage.setFrozen(count);
            tccStorage.setStatus(0); // PENDING
            tccStorageMapper.insert(tccStorage);
        } else {
            // 既存のTCCレコードを更新
            existingTccStorage.setXid(xid);
            existingTccStorage.setFrozen(existingTccStorage.getFrozen() + count);
            existingTccStorage.setResidue(existingTccStorage.getResidue() - count);
            existingTccStorage.setStatus(0); // PENDING
            tccStorageMapper.updateById(existingTccStorage);
        }
        
        log.info("TRY completed: 在庫を凍結しました");
        return true;
    }

    /**
     * TCC Confirm: 凍結を確定
     */
    @Override
//    @Transactional
    public boolean confirm(BusinessActionContext context) {
        String xid = context.getXid();
        Integer count = context.getActionContext("count")==null? null:(Integer)context.getActionContext("count");
       Object productIdObj = context.getActionContext("productId");
       Long productId = null;
       if (productIdObj != null) {
           if (productIdObj instanceof Number) {
               productId = ((Number) productIdObj).longValue();
           } else {
               try {
                   productId = Long.valueOf(productIdObj.toString());
               } catch (NumberFormatException ignored) {
                   productId = null;
               }
           }
       }
        log.info("=== TCC CONFIRM (Storage) === xid={}, count={}, productId={}", xid, count, productId);
        
//        TccStorage tccStorage = tccStorageMapper.selectOne(
//            new LambdaQueryWrapper<TccStorage>().eq(TccStorage::getXid, xid));

        TccStorage tccStorage = tccStorageMapper.selectOne(
                new LambdaQueryWrapper<TccStorage>().eq(TccStorage::getXid, xid));

        if (tccStorage == null) {
            log.warn("CONFIRM: TCCレコードが見つかりません: xid={}", xid);
            return true;
        }
        
        if (tccStorage.getStatus() == 1) {
            log.info("CONFIRM already executed (idempotent): xid={}", xid);
            return true;
        }
        
        // 凍結分を実際の使用分に移動
        tccStorage.setUsed(tccStorage.getUsed() + tccStorage.getFrozen());
        tccStorage.setFrozen(0); // 凍結分をクリア
        
        // ステータスを確定に更新
        tccStorage.setStatus(1); // SUCCESS
        tccStorageMapper.updateById(tccStorage);
        
        log.info("CONFIRM completed: 在庫減算を確定しました");
        return true;
    }

    /**
     * TCC Cancel: 凍結を取り消し
     */
    @Override
//    @Transactional
    public boolean cancel(BusinessActionContext context) {
        String xid = context.getXid();
        Integer count = context.getActionContext("count")==null? null:(Integer)context.getActionContext("count");
        Long productId = (Long) context.getActionContext("productId");
        log.info("=== TCC CANCEL (Storage) === xid={}, count={}, productId={}", xid, count, productId);
        
        TccStorage tccStorage = tccStorageMapper.selectOne(
            new LambdaQueryWrapper<TccStorage>().eq(TccStorage::getXid, xid));
        
        if (tccStorage == null) {
            log.info("CANCEL: TCCレコードが見つかりません（空振り）: xid={}", xid);
            return true;
        }
        
        if (tccStorage.getStatus() == 2) {
            log.info("CANCEL already executed (idempotent): xid={}", xid);
            return true;
        }

        // Integer count = (Integer) context.getActionContext("count");
//        log.info("productId={}", productId);
        
        // 凍結分を在庫に戻す
        tccStorage.setResidue(tccStorage.getResidue() + tccStorage.getFrozen());
        tccStorage.setFrozen(0); // 凍結分をクリア
        
        // ステータスをキャンセルに更新
        tccStorage.setStatus(2); // FAILED
        tccStorageMapper.updateById(tccStorage);
        
        log.info("CANCEL completed: 凍結を取り消しました");
        return true;
    }
}