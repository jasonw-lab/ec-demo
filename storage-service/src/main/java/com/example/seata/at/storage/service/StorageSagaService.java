package com.example.seata.at.storage.service;

public interface StorageSagaService {
    boolean deduct(Long productId, Integer count, String orderNo);
    boolean compensate(Long productId, Integer count, String orderNo);
}

