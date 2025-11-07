package com.example.seata.at.storage.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.storage.domain.entity.Storage;
import com.example.seata.at.storage.domain.mapper.StorageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageATServiceImpl implements StorageATService {
    private final StorageMapper storageMapper;

    public StorageATServiceImpl(StorageMapper storageMapper) {
        this.storageMapper = storageMapper;
    }

    @Transactional
    public void deduct(Long productId, Integer count) {
        LambdaUpdateWrapper<Storage> uw = new LambdaUpdateWrapper<>();
        uw.eq(Storage::getProductId, productId)
          .ge(Storage::getResidue, count)
          .setSql("used = used + " + count)
          .setSql("residue = residue - " + count);
        int updated = storageMapper.update(null, uw);
        if (updated == 0) {
            throw new InsufficientStockException("Insufficient stock for productId=" + productId + ", count=" + count);
        }
    }

    public boolean checkStock(Long productId, Integer count) {
        Storage storage = storageMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Storage>()
                .eq(Storage::getProductId, productId));
        return storage != null && storage.getResidue() >= count;
    }

    // Note: refund is part of Saga service to avoid changing AT service contract

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) { super(message); }
    }
}
