package com.example.seata.at.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seata.at.account.domain.entity.TccAccount;
import com.example.seata.at.account.domain.mapper.TccAccountMapper;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Account の TCC サービス実装
 */
@Service
public class AccountTccServiceImpl implements AccountTccService {
    private static final Logger log = LoggerFactory.getLogger(AccountTccServiceImpl.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final TccAccountMapper tccAccountMapper;

    public AccountTccServiceImpl(TccAccountMapper tccAccountMapper) {
        this.tccAccountMapper = tccAccountMapper;
    }

    /**
     * 残高チェック
     */
    @Override
    public boolean checkBalance(Long userId, BigDecimal amount) {
        TccAccount tccAccount = tccAccountMapper.selectOne(
            new LambdaQueryWrapper<TccAccount>().eq(TccAccount::getUserId, userId));
        return tccAccount != null && tccAccount.getResidue().compareTo(amount) >= 0;
    }

    /**
     * TCC Try: 残高を凍結
     */
    @Override
    @Transactional
    public boolean tryDebit(Long userId, BigDecimal amount, Long orderId) {
        String xid = RootContext.getXID();
        log.info("=== TCC TRY (Account) === xid={}, userId={}, amount={}, orderId={}", xid, userId, amount, orderId);
        
        // 冪等性チェック
        TccAccount existing = tccAccountMapper.selectOne(
            new LambdaQueryWrapper<TccAccount>().eq(TccAccount::getXid, xid));
        if (existing != null) {
            log.info("TRY already executed (idempotent): xid={}", xid);
            return true;
        }
        
        // 残高チェック
        if (!checkBalance(userId, amount)) {
            log.warn("TRY failed: 残高不足 userId={}, amount={}", userId, amount);
            throw new RuntimeException("残高不足");
        }
        
        // 残高を凍結
        TccAccount existingTccAccount = tccAccountMapper.selectOne(
            new LambdaQueryWrapper<TccAccount>().eq(TccAccount::getUserId, userId));
        
        if (existingTccAccount == null) {
            // 新しいTCCレコードを作成
            TccAccount tccAccount = new TccAccount();
            tccAccount.setXid(xid);
            tccAccount.setOrderId(orderId);
            tccAccount.setUserId(userId);
            tccAccount.setTotal(BigDecimal.ZERO); // 初期値
            tccAccount.setUsed(BigDecimal.ZERO); // 初期値
            tccAccount.setResidue(BigDecimal.ZERO); // 初期値
            tccAccount.setFrozen(amount);
            tccAccount.setStatus(STATUS_PENDING); // PENDING
            tccAccountMapper.insert(tccAccount);
        } else {
            // 既存のTCCレコードを更新
            existingTccAccount.setXid(xid);
            existingTccAccount.setOrderId(orderId);
            existingTccAccount.setFrozen(existingTccAccount.getFrozen().add(amount));
            existingTccAccount.setResidue(existingTccAccount.getResidue().subtract(amount));
            existingTccAccount.setStatus(STATUS_PENDING); // PENDING
            tccAccountMapper.updateById(existingTccAccount);
        }
        
        log.info("TRY completed: 残高を凍結しました");
        return true;
    }

    /**
     * TCC Confirm: 凍結を確定
     */
    @Override
//    @Transactional
    public boolean confirm(BusinessActionContext context) {
        String xid = context.getXid();
        Long orderId = (Long) context.getActionContext("orderId");
//        String orderNo = String.valueOf( context.getActionContext("orderNo"));
        String userId = String.valueOf( context.getActionContext("userId"));
        log.info("=== TCC CONFIRM (Account) === xid={},  orderId={}, userId={}", xid, orderId, userId);
        
        TccAccount tccAccount = tccAccountMapper.selectOne(
            new LambdaQueryWrapper<TccAccount>().eq(TccAccount::getXid, xid));
        
        if (tccAccount == null) {
            log.warn("CONFIRM: TCCレコードが見つかりません: xid={}", xid);
            return true;
        }
        
        if (STATUS_SUCCESS.equals(tccAccount.getStatus())) {
            log.info("CONFIRM already executed (idempotent): xid={}", xid);
            return true;
        }

        tccAccount.setOrderId(orderId);

        // 凍結分を実際の使用分に移動
        tccAccount.setUsed(tccAccount.getUsed().add(tccAccount.getFrozen()));
        tccAccount.setFrozen(BigDecimal.ZERO); // 凍結分をクリア
        
        // ステータスを確定に更新
        tccAccount.setStatus(STATUS_SUCCESS); // SUCCESS
        tccAccountMapper.updateById(tccAccount);
        
        log.info("CONFIRM completed: 残高減算を確定しました");
        return true;
    }

    /**
     * TCC Cancel: 凍結を取り消し
     */
    @Override
//    @Transactional
    public boolean cancel(BusinessActionContext context) {
        String xid = context.getXid();
        Long orderId = (Long) context.getActionContext("orderId");
        log.info("=== TCC CANCEL (Account) === xid={}, orderId={}", xid, orderId);

        
        TccAccount tccAccount = tccAccountMapper.selectOne(
            new LambdaQueryWrapper<TccAccount>().eq(TccAccount::getXid, xid));
        
        if (tccAccount == null) {
            log.info("CANCEL: TCCレコードが見つかりません（空振り）: xid={}", xid);
            return true;
        }
        
        if (STATUS_FAILED.equals(tccAccount.getStatus())) {
            log.info("CANCEL already executed (idempotent): xid={}", xid);
            return true;
        }
        
        // 凍結分を残高に戻す
        tccAccount.setResidue(tccAccount.getResidue().add(tccAccount.getFrozen()));
        tccAccount.setFrozen(BigDecimal.ZERO); // 凍結分をクリア
        
        // ステータスをキャンセルに更新
        tccAccount.setStatus(STATUS_FAILED); // FAILED
        tccAccountMapper.updateById(tccAccount);
        
        log.info("CANCEL completed: 凍結を取り消しました");
        return true;
    }
}