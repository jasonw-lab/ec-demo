package com.demo.ec.es.controller;

import com.demo.ec.es.model.SearchResponse;
import com.demo.ec.es.model.SearchSort;
import com.demo.ec.es.model.SuggestResponse;
import com.demo.ec.es.service.SearchService;
import com.demo.ec.es.service.SuggestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for product search and suggestion APIs.
 * Handles search queries with filtering, sorting, and pagination.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);
    
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
            @RequestParam(value = "sort", defaultValue = "relevance") String sortParam,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        log.debug("Searching products: q={}, minPrice={}, maxPrice={}, sort={}, page={}, size={}",
                q, minPrice, maxPrice, sortParam, page, size);
        
        SearchSort sort = SearchSort.fromString(sortParam);
        return searchService.search(q, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/suggest")
    public SuggestResponse suggest(
            @RequestParam("q") String q,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        log.debug("Fetching suggestions: q={}, size={}", q, size);
        return new SuggestResponse(suggestService.suggest(q, size));
    }
}
