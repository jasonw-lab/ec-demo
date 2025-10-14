package com.example.seata.at.storage.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Storage TCC Service Interface
 */
@LocalTCC
public interface StorageTccService {
    
    /**
     * 在庫チェック
     * @param productId 商品ID
     * @param count 数量
     * @return 在庫が十分かどうか
     */
    boolean checkStock(Long productId, Integer count);
    
    /**
     * TCC Try: 在庫を凍結
     * @param productId 商品ID
     * @param count 数量
     * @return 成功かどうか
     */
    @TwoPhaseBusinessAction(name = "deductInventory", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryDeduct(@BusinessActionContextParameter(paramName = "productId") Long productId,
                      @BusinessActionContextParameter(paramName = "count") Integer count);
    
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