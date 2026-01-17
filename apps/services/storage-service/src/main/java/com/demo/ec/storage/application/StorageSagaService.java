package com.demo.ec.storage.application;

public interface StorageSagaService {
    boolean deduct(Long productId, Integer count, String orderNo);
    boolean compensate(Long productId, Integer count, String orderNo);
    boolean confirm(Long productId, Integer count, String orderNo);
}
