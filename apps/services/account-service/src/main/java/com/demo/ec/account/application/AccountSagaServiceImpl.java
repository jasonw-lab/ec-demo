package com.demo.ec.account.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountSagaServiceImpl implements AccountSagaService {
    private static final Logger log = LoggerFactory.getLogger(AccountSagaServiceImpl.class);

    private final com.demo.ec.account.gateway.AccountMapper accountMapper;

    public AccountSagaServiceImpl(com.demo.ec.account.gateway.AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional
    public void debit(Long userId, BigDecimal amount, String orderNo) {
        log.info("[SAGA][Account] debit begin: orderNo={}, userId={}, amount={}", orderNo, userId, amount);
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.demo.ec.account.domain.Account> uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(com.demo.ec.account.domain.Account::getUserId, userId)
          .ge(com.demo.ec.account.domain.Account::getResidue, amount)
          .setSql("used = used + " + amount)
          .setSql("residue = residue - " + amount);
        int updated = accountMapper.update(null, uw);
        if (updated == 0) {
            throw new RuntimeException("Insufficient balance for userId=" + userId + ", amount=" + amount);
        }
        log.info("[SAGA][Account] debit success: orderNo={}", orderNo);
    }

    @Override
    @Transactional
    public void compensate(Long userId, BigDecimal amount, String orderNo) {
        log.info("[SAGA][Account] compensate begin: orderNo={}, userId={}, amount={}", orderNo, userId, amount);
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.demo.ec.account.domain.Account> uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(com.demo.ec.account.domain.Account::getUserId, userId)
          .setSql("used = used - " + amount)
          .setSql("residue = residue + " + amount);
        accountMapper.update(null, uw);
        log.info("[SAGA][Account] compensate success: orderNo={}", orderNo);
    }
}


