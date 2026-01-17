package com.demo.ec.es.web;

import com.demo.ec.es.domain.ImportRequest;
import com.demo.ec.es.domain.ImportResult;
import com.demo.ec.es.application.ImportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API controller for product data import.
 * Handles CSV import with image upload to MinIO and indexing to Elasticsearch.
 */
@RestController
@RequestMapping("/internal/products")
public class InternalImportController {
    private static final Logger log = LoggerFactory.getLogger(InternalImportController.class);
    
    private final ImportService importService;

    public InternalImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public ImportResult importCsv(@Valid @RequestBody ImportRequest request) {
        log.info("Starting CSV import: csvPath={}, imagesDir={}, batchSize={}",
                request.csvPath(), request.imagesDir(), request.batchSize());
        
        ImportResult result = importService.importCsv(request);
        
        log.info("Import completed: total={}, success={}, failed={}, errors={}",
                result.total(), result.success(), result.failed(), result.errors().size());
        
        return result;
    }
}
