package com.demo.ec.storage.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Application startup hook that preloads product data into Redis.
@Component
public class ProductCacheWarmupRunner implements ApplicationRunner {

    private final ProductCacheService productCacheService;

    public ProductCacheWarmupRunner(ProductCacheService productCacheService) {
        this.productCacheService = productCacheService;
    }

    @Override
    public void run(ApplicationArguments args) {
        productCacheService.preloadAll();
    }
}
