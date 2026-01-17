package com.demo.ec.account.application;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

/**
 * Account TCC Service Interface
 */
@LocalTCC
public interface AccountTccService {
    
    /**
     * 残高チェック
     * @param userId ユーザーID
     * @param amount 金額
     * @return 残高が十分かどうか
     */
    boolean checkBalance(Long userId, BigDecimal amount);
    
    /**
     * TCC Try: 残高を凍結
     * @param userId ユーザーID
     * @param amount 金額
     * @return 成功かどうか
     */
    @TwoPhaseBusinessAction(name = "debitAccount", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryDebit(@BusinessActionContextParameter(paramName = "userId") Long userId,
                     @BusinessActionContextParameter(paramName = "amount") BigDecimal amount,
//                     @BusinessActionContextParameter(paramName = "orderNo") Long orderNo),
                     @BusinessActionContextParameter(paramName = "orderId") Long orderId);
    
    /**
     * TCC Confirm: 凍結を確定
     * @param context ビジネスアクションコンテキスト
     * @return 成功かどうか
     */
    boolean confirm(BusinessActionContext context);
    
    /**
     * TCC Cancel: 凍結を取り消し
     * @param context ビジネスアクションコンテキスト
     * @return 成功かどうか
     */
    boolean cancel(BusinessActionContext context);
}