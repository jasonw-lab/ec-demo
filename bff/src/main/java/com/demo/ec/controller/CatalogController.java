package com.demo.ec.controller;

import com.demo.ec.client.ProductSearchClient;
import com.demo.ec.client.dto.SearchResponse;
import com.demo.ec.client.dto.SuggestResponse;
import com.demo.ec.model.Category;
import com.demo.ec.model.Product;
import com.demo.ec.repo.DemoData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ProductSearchClient productSearchClient;

    public CatalogController(ProductSearchClient productSearchClient) {
        this.productSearchClient = productSearchClient;
    }

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return DemoData.categories;
    }

    @GetMapping("/products")
    public List<Product> getProducts(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        if (categoryId == null) return DemoData.products;
        return DemoData.products.stream().filter(p -> p.categoryId().equals(categoryId)).collect(Collectors.toList());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return DemoData.products.stream().filter(p -> p.id().equals(id)).findFirst()
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/search")
    public SearchResponse searchProducts(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "minPrice", required = false) Long minPrice,
            @RequestParam(value = "maxPrice", required = false) Long maxPrice,
            @RequestParam(value = "sort", defaultValue = "relevance") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return productSearchClient.searchProducts(q, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/products/suggest")
    public SuggestResponse suggestProducts(
            @RequestParam(value = "q") String q,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return productSearchClient.suggest(q, size);
    }
}
