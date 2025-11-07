package com.example.seata.at.storage.service;

/**
 * Storage AT Service Interface
 */
public interface StorageATService {
    
    /**
     * 在庫を減算する
     * @param productId 商品ID
     * @param count 数量
     */
    void deduct(Long productId, Integer count);
    
    /**
     * 在庫をチェックする
     * @param productId 商品ID
     * @param count 数量
     * @return 在庫が十分かどうか
     */
    boolean checkStock(Long productId, Integer count);

}
