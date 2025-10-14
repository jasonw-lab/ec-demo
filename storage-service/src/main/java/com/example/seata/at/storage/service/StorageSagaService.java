package com.example.seata.at.storage.service;

public interface StorageSagaService {
    void deduct(Long productId, Integer count, String orderNo);
    void compensate(Long productId, Integer count, String orderNo);
}


