package com.example.seata.at.storage.api;

import com.example.seata.at.storage.api.dto.CommonResponse;
import com.example.seata.at.storage.api.dto.DeductRequest;
import com.example.seata.at.storage.service.StorageATService;
import com.example.seata.at.storage.service.StorageATServiceImpl;
import com.example.seata.at.storage.service.StorageTccService;
import com.example.seata.at.storage.service.StorageSagaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private static final Logger log = LoggerFactory.getLogger(StorageController.class);

    private final StorageATService storageATService;
    private final StorageTccService storageTccService;
    private final StorageSagaService storageSagaService;

    public StorageController(StorageATService storageATService, StorageTccService storageTccService, StorageSagaService storageSagaService) {
        this.storageATService = storageATService;
        this.storageTccService = storageTccService;
        this.storageSagaService = storageSagaService;
    }

    @PostMapping("/deduct")
    public CommonResponse<String> deduct(@Valid @RequestBody DeductRequest req) {
        // Log received request body at INFO level
        log.info("Received DeductRequest: productId={}, count={}", req.getProductId(), req.getCount());
        storageATService.deduct(req.getProductId(), req.getCount());
        return CommonResponse.ok("deducted");
    }

    @PostMapping("/deduct/tcc")
    public CommonResponse<String> deductTcc(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            orderNo = java.util.UUID.randomUUID().toString();
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received DeductRequest TCC: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        storageTccService.tryDeduct(req.getProductId(), req.getCount());
        return CommonResponse.ok("tcc-try-deducted");
    }

    @PostMapping("/deduct/saga")
    public CommonResponse<String> deductSaga(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return CommonResponse.fail("orderNo is required for saga operations");
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received DeductRequest SAGA: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        try {
            boolean ok = storageSagaService.deduct(req.getProductId(), req.getCount(), orderNo);
            return ok ? CommonResponse.ok("saga-deducted") : CommonResponse.fail("deduct failed");
        } catch (StorageATServiceImpl.InsufficientStockException | IllegalArgumentException ex) {
            log.warn("Saga deduct failed orderNo={} reason={}", orderNo, ex.getMessage());
            return CommonResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/compensate/saga")
    public CommonResponse<String> compensateSaga(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return CommonResponse.fail("orderNo is required for saga operations");
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received CompensateRequest SAGA: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        try {
            boolean ok = storageSagaService.compensate(req.getProductId(), req.getCount(), orderNo);
            return ok ? CommonResponse.ok("saga-compensated") : CommonResponse.fail("compensate failed");
        } catch (IllegalArgumentException ex) {
            log.warn("Saga compensate failed orderNo={} reason={}", orderNo, ex.getMessage());
            return CommonResponse.fail(ex.getMessage());
        }
    }

    @ExceptionHandler(StorageATServiceImpl.InsufficientStockException.class)
    public ResponseEntity<CommonResponse<Void>> handleStock(StorageATServiceImpl.InsufficientStockException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }
}
