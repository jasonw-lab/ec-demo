package com.example.seata.at.storage.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.storage.domain.entity.Storage;
import com.example.seata.at.storage.domain.entity.StorageTxStepLog;
import com.example.seata.at.storage.domain.mapper.StorageMapper;
import com.example.seata.at.storage.domain.mapper.StorageTxStepLogMapper;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageSagaServiceImpl implements StorageSagaService {
    private static final Logger log = LoggerFactory.getLogger(StorageSagaServiceImpl.class);

    private final StorageMapper storageMapper;
    private final StorageTxStepLogMapper txStepLogMapper;

    private static final String STEP_DEDUCT = "STORAGE_DEDUCT";
    private static final String STEP_COMPENSATE = "STORAGE_COMPENSATE";
    private static final String STEP_CONFIRM = "STORAGE_CONFIRM";

    public StorageSagaServiceImpl(StorageMapper storageMapper, StorageTxStepLogMapper txStepLogMapper) {
        this.storageMapper = storageMapper;
        this.txStepLogMapper = txStepLogMapper;
    }

    @Override
    @Transactional
    public boolean deduct(Long productId, Integer count, String orderNo) {
        log.info("[SAGA][Storage] deduct begin: orderNo={}, productId={}, count={}", orderNo, productId, count);
        Long logId = createStepIfAbsent(orderNo, STEP_DEDUCT);
        if (logId == null) {
            log.info("[SAGA][Storage] deduct already processed. orderNo={}", orderNo);
            return true;
        }

        LambdaUpdateWrapper<Storage> uw = new LambdaUpdateWrapper<>();
        uw.eq(Storage::getProductId, productId)
          .ge(Storage::getResidue, count)
          .setSql("used = used + " + count)
          .setSql("residue = residue - " + count);
        int updated = storageMapper.update(null, uw);
        if (updated == 0) {
            log.warn("[SAGA][Storage] deduct insufficient stock orderNo={} productId={} count={}", orderNo, productId, count);
            throw new StorageATServiceImpl.InsufficientStockException("Insufficient stock for productId=" + productId + ", count=" + count);
        }
        markStepDone(logId);
        log.info("[SAGA][Storage] deduct success: orderNo={}", orderNo);
        return true;
    }

    @Override
    @Transactional
    public boolean compensate(Long productId, Integer count, String orderNo) {
        log.info("[SAGA][Storage] compensate begin: orderNo={}, productId={}, count={}", orderNo, productId, count);
        Long logId = createStepIfAbsent(orderNo, STEP_COMPENSATE);
        if (logId == null) {
            log.info("[SAGA][Storage] compensate already processed. orderNo={}", orderNo);
            return true;
        }
        LambdaUpdateWrapper<Storage> uw = new LambdaUpdateWrapper<>();
        uw.eq(Storage::getProductId, productId)
          .setSql("used = CASE WHEN used >= " + count + " THEN used - " + count + " ELSE 0 END")
          .setSql("residue = residue + " + count);
        storageMapper.update(null, uw);
        markStepDone(logId);
        log.info("[SAGA][Storage] compensate success: orderNo={}", orderNo);
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(Long productId, Integer count, String orderNo) {
        log.info("[SAGA][Storage] confirm begin: orderNo={}, productId={}, count={}", orderNo, productId, count);
        Long logId = createStepIfAbsent(orderNo, STEP_CONFIRM);
        if (logId == null) {
            log.info("[SAGA][Storage] confirm already processed. orderNo={}", orderNo);
            return true;
        }
        // Reservation already deducted residue and added to used at reserve step.
        // Confirm step simply records completion for idempotency/traceability.
        markStepDone(logId);
        log.info("[SAGA][Storage] confirm success: orderNo={}", orderNo);
        return true;
    }

    private Long createStepIfAbsent(String orderNo, String step) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new IllegalArgumentException("orderNo must not be blank");
        }
        StorageTxStepLog logEntry = new StorageTxStepLog();
        logEntry.setOrderNo(orderNo);
        logEntry.setStep(step);
        logEntry.setStatus("PROCESSING");
        try {
            int inserted = txStepLogMapper.insert(logEntry);
            return inserted == 1 ? logEntry.getId() : null;
        } catch (DuplicateKeyException ex) {
            return null;
        }
    }

    private void markStepDone(Long id) {
        LambdaUpdateWrapper<StorageTxStepLog> uw = new LambdaUpdateWrapper<>();
        uw.eq(StorageTxStepLog::getId, id)
          .set(StorageTxStepLog::getStatus, "DONE");
        txStepLogMapper.update(null, uw);
    }
}
