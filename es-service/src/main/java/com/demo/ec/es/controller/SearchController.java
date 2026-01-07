package com.demo.ec.es.controller;

import com.demo.ec.es.model.SearchResponse;
import com.demo.ec.es.model.SearchSort;
import com.demo.ec.es.model.SuggestResponse;
import com.demo.ec.es.service.SearchService;
import com.demo.ec.es.service.SuggestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;
    private final SuggestService suggestService;

    public SearchController(SearchService searchService, SuggestService suggestService) {
        this.searchService = searchService;
        this.suggestService = suggestService;
    }

    @GetMapping("/products")
    public SearchResponse searchProducts(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "minPrice", required = false) Long minPrice,
            @RequestParam(value = "maxPrice", required = false) Long maxPrice,
            @RequestParam(value = "sort", required = false) SearchSort sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) throws IOException {
        return searchService.search(q, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/suggest")
    public SuggestResponse suggest(@RequestParam("q") String q,
                                   @RequestParam(value = "size", defaultValue = "10") int size) throws IOException {
        return new SuggestResponse(suggestService.suggest(q, size));
    }
}
