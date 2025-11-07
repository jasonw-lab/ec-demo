package com.example.seata.at.account.service;

import java.math.BigDecimal;

public interface AccountSagaService {
    void debit(Long userId, BigDecimal amount, String orderNo);
    void compensate(Long userId, BigDecimal amount, String orderNo);
}


