package com.demo.ec.controller;

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
}
