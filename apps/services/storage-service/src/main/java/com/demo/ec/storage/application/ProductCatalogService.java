package com.demo.ec.storage.application;

import com.demo.ec.storage.web.dto.ProductResponse;
import com.demo.ec.storage.domain.Product;
import com.demo.ec.storage.gateway.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Product catalog read service with Redis cache lookup before DB fallback.
@Service
public class ProductCatalogService {
    private static final Logger log = LoggerFactory.getLogger(ProductCatalogService.class);

    private final ProductMapper productMapper;
    private final ProductCacheService productCacheService;

    public ProductCatalogService(ProductMapper productMapper, ProductCacheService productCacheService) {
        this.productMapper = productMapper;
        this.productCacheService = productCacheService;
    }

    public Optional<ProductResponse> findById(Long productId) {
        Optional<ProductResponse> cached = productCacheService.get(productId);
        if (cached.isPresent()) {
            return cached;
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            log.debug("Product not found in storage DB: {}", productId);
            return Optional.empty();
        }
        ProductResponse response = toResponse(product);
        productCacheService.put(response);
        return Optional.of(response);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice()
        );
    }
}
