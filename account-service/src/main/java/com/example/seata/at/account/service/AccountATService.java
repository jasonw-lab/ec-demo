package com.example.seata.at.account.service;

import java.math.BigDecimal;

/**
 * Account AT Service Interface
 */
public interface AccountATService {
    
    /**
     * 残高を減算する
     * @param userId ユーザーID
     * @param amount 金額
     */
    void debit(Long userId, BigDecimal amount);
    
    /**
     * 残高をチェックする
     * @param userId ユーザーID
     * @param amount 金額
     * @return 残高が十分かどうか
     */
    boolean checkBalance(Long userId, BigDecimal amount);

    // Note: refund is provided by Saga service to keep AT contract unchanged
}
