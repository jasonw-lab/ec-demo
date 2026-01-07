package com.demo.ec.es.controller;

import com.demo.ec.es.model.ImportRequest;
import com.demo.ec.es.model.ImportResult;
import com.demo.ec.es.service.ImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/internal/products")
public class InternalImportController {
    private final ImportService importService;

    public InternalImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public ImportResult importCsv(@Valid @RequestBody ImportRequest request) throws IOException {
        return importService.importCsv(request);
    }
}
