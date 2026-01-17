package com.example.seata.at.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seata.at.storage.domain.entity.Storage;
import com.example.seata.at.storage.domain.mapper.StorageMapper;
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
