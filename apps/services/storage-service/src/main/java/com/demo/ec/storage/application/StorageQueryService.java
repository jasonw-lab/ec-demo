package com.demo.ec.storage.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.ec.storage.domain.Storage;
import com.demo.ec.storage.gateway.StorageMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StorageQueryService {
    private final StorageMapper storageMapper;

    public StorageQueryService(StorageMapper storageMapper) {
        this.storageMapper = storageMapper;
    }

    public Optional<Storage> findByProductId(Long productId) {
        Storage storage = storageMapper.selectOne(
                new LambdaQueryWrapper<Storage>().eq(Storage::getProductId, productId));
        return Optional.ofNullable(storage);
    }
}
