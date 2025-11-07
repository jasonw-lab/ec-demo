package com.example.seata.at.account.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.account.domain.entity.Account;
import com.example.seata.at.account.domain.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountATServiceImpl implements AccountATService {
    private final AccountMapper accountMapper;

    public AccountATServiceImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        LambdaUpdateWrapper<Account> uw = new LambdaUpdateWrapper<>();
        uw.eq(Account::getUserId, userId)
          .ge(Account::getResidue, amount)
          .setSql("used = used + " + amount)
          .setSql("residue = residue - " + amount);
        int updated = accountMapper.update(null, uw);
        if (updated == 0) {
            throw new InsufficientBalanceException("Insufficient balance for userId=" + userId + ", amount=" + amount);
        }
    }

    public boolean checkBalance(Long userId, BigDecimal amount) {
        Account account = accountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId));
        return account != null && account.getResidue().compareTo(amount) >= 0;
    }

    // Note: refund is part of Saga service to avoid changing AT service contract

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) { super(message); }
    }
}
