package com.example.seata.at.storage.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.storage.domain.entity.Storage;
import com.example.seata.at.storage.domain.mapper.StorageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageSagaServiceImpl implements StorageSagaService {
    private static final Logger log = LoggerFactory.getLogger(StorageSagaServiceImpl.class);

    private final StorageATService storageATService;
    private final StorageMapper storageMapper;

    public StorageSagaServiceImpl(StorageATService storageATService, StorageMapper storageMapper) {
        this.storageATService = storageATService;
        this.storageMapper = storageMapper;
    }

    @Override
    @Transactional
    public void deduct(Long productId, Integer count, String orderNo) {
        log.info("[SAGA][Storage] deduct begin: orderNo={}, productId={}, count={}", orderNo, productId, count);
        storageATService.deduct(productId, count);
        log.info("[SAGA][Storage] deduct success: orderNo={}", orderNo);
    }

    @Override
    @Transactional
    public void compensate(Long productId, Integer count, String orderNo) {
        log.info("[SAGA][Storage] compensate begin: orderNo={}, productId={}, count={}", orderNo, productId, count);
        LambdaUpdateWrapper<Storage> uw = new LambdaUpdateWrapper<>();
        uw.eq(Storage::getProductId, productId)
          .setSql("used = used - " + count)
          .setSql("residue = residue + " + count);
        storageMapper.update(null, uw);
        log.info("[SAGA][Storage] compensate success: orderNo={}", orderNo);
    }
}


