package com.demo.ec.es.controller;

import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.model.ReindexRequest;
import com.demo.ec.es.service.IndexAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/internal/products")
public class InternalIndexController {
    private final IndexAdminService indexAdminService;
    private final EsServiceProperties properties;

    public InternalIndexController(IndexAdminService indexAdminService, EsServiceProperties properties) {
        this.indexAdminService = indexAdminService;
        this.properties = properties;
    }

    @PostMapping("/index/init")
    public Map<String, String> initIndex() throws IOException {
        indexAdminService.initIndexIfMissing();
        return Map.of("index", properties.getIndex().getName());
    }

    @PostMapping("/reindex")
    public Map<String, String> reindex(@Valid @RequestBody ReindexRequest request) throws IOException {
        String alias = request.alias() == null || request.alias().isBlank() ? properties.getIndex().getAlias() : request.alias();
        indexAdminService.reindex(request.sourceIndex(), request.targetIndex(), alias);
        return Map.of("source", request.sourceIndex(), "target", request.targetIndex(), "alias", alias);
    }
}
