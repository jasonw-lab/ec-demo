package com.example.seata.at.storage.api;

import com.example.seata.at.storage.api.dto.CommonResponse;
import com.example.seata.at.storage.api.dto.DeductRequest;
import com.example.seata.at.storage.api.dto.ProductResponse;
import com.example.seata.at.storage.api.dto.StockResponse;
import com.example.seata.at.storage.service.StorageATService;
import com.example.seata.at.storage.service.StorageATServiceImpl;
import com.example.seata.at.storage.service.StorageTccService;
import com.example.seata.at.storage.service.StorageSagaService;
import com.example.seata.at.storage.service.ProductCatalogService;
import com.example.seata.at.storage.service.StorageQueryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private static final Logger log = LoggerFactory.getLogger(StorageController.class);

    private final StorageATService storageATService;
    private final StorageTccService storageTccService;
    private final StorageSagaService storageSagaService;
    private final ProductCatalogService productCatalogService;
    private final StorageQueryService storageQueryService;

    public StorageController(StorageATService storageATService, StorageTccService storageTccService,
                             StorageSagaService storageSagaService, ProductCatalogService productCatalogService,
                             StorageQueryService storageQueryService) {
        this.storageATService = storageATService;
        this.storageTccService = storageTccService;
        this.storageSagaService = storageSagaService;
        this.productCatalogService = productCatalogService;
        this.storageQueryService = storageQueryService;
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

    @PostMapping("/reserve/saga")
    public CommonResponse<String> reserveSaga(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return CommonResponse.fail("orderNo is required for saga operations");
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received ReserveRequest SAGA: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        try {
            boolean ok = storageSagaService.deduct(req.getProductId(), req.getCount(), orderNo);
            return ok ? CommonResponse.ok("saga-reserved") : CommonResponse.fail("reserve failed");
        } catch (StorageATServiceImpl.InsufficientStockException | IllegalArgumentException ex) {
            log.warn("Saga reserve failed orderNo={} reason={}", orderNo, ex.getMessage());
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

    @PostMapping("/release/saga")
    public CommonResponse<String> releaseSaga(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return CommonResponse.fail("orderNo is required for saga operations");
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received ReleaseRequest SAGA: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        try {
            boolean ok = storageSagaService.compensate(req.getProductId(), req.getCount(), orderNo);
            return ok ? CommonResponse.ok("saga-released") : CommonResponse.fail("release failed");
        } catch (IllegalArgumentException ex) {
            log.warn("Saga release failed orderNo={} reason={}", orderNo, ex.getMessage());
            return CommonResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/confirm/saga")
    public CommonResponse<String> confirmSaga(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return CommonResponse.fail("orderNo is required for saga operations");
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received ConfirmRequest SAGA: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        try {
            boolean ok = storageSagaService.confirm(req.getProductId(), req.getCount(), orderNo);
            return ok ? CommonResponse.ok("saga-confirmed") : CommonResponse.fail("confirm failed");
        } catch (IllegalArgumentException ex) {
            log.warn("Saga confirm failed orderNo={} reason={}", orderNo, ex.getMessage());
            return CommonResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/commit/saga")
    public CommonResponse<String> commitSaga(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return CommonResponse.fail("orderNo is required for saga operations");
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received CommitRequest SAGA: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        try {
            boolean ok = storageSagaService.confirm(req.getProductId(), req.getCount(), orderNo);
            return ok ? CommonResponse.ok("saga-committed") : CommonResponse.fail("commit failed");
        } catch (IllegalArgumentException ex) {
            log.warn("Saga commit failed orderNo={} reason={}", orderNo, ex.getMessage());
            return CommonResponse.fail(ex.getMessage());
        }
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<CommonResponse<ProductResponse>> getProduct(@PathVariable("productId") Long productId) {
        return productCatalogService.findById(productId)
                .map(product -> ResponseEntity.ok(CommonResponse.ok(product)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResponse.fail("Product not found: " + productId)));
    }

    @GetMapping("/stocks/{productId}")
    public ResponseEntity<CommonResponse<StockResponse>> getStock(@PathVariable("productId") Long productId) {
        return storageQueryService.findByProductId(productId)
                .map(storage -> ResponseEntity.ok(CommonResponse.ok(
                        new StockResponse(storage.getProductId(), storage.getTotal(),
                                storage.getUsed(), storage.getResidue(), storage.getFrozen()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResponse.fail("Stock not found for productId: " + productId)));
    }

    @ExceptionHandler(StorageATServiceImpl.InsufficientStockException.class)
    public ResponseEntity<CommonResponse<Void>> handleStock(StorageATServiceImpl.InsufficientStockException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }
}
