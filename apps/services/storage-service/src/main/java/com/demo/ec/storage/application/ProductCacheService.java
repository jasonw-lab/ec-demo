package com.demo.ec.storage.application;

import com.demo.ec.storage.web.dto.ProductResponse;
import com.demo.ec.storage.domain.Product;
import com.demo.ec.storage.gateway.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

// Redis-backed cache for product master data with DB preload support.
@Service
public class ProductCacheService {
    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);
    private static final String KEY_PREFIX = "product:";

    private final RedisTemplate<String, ProductResponse> redisTemplate;
    private final ProductMapper productMapper;
    private final Duration ttl;

    public ProductCacheService(
            RedisTemplate<String, ProductResponse> redisTemplate,
            ProductMapper productMapper,
            @Value("${app.product-cache.ttl-seconds:1800}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.productMapper = productMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<ProductResponse> get(Long productId) {
        try {
            ProductResponse cached = redisTemplate.opsForValue().get(key(productId));
            return Optional.ofNullable(cached);
        } catch (Exception ex) {
            log.warn("Product cache read failed productId={} err={}", productId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(ProductResponse product) {
        if (product == null || product.getId() == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(product.getId()), product, ttl);
        } catch (Exception ex) {
            log.warn("Product cache write failed productId={} err={}", product.getId(), ex.getMessage());
        }
    }

    public void preloadAll() {
        try {
            List<Product> products = productMapper.selectList(null);
            for (Product product : products) {
                put(toResponse(product));
            }
            log.info("Product cache preload complete size={}", products.size());
        } catch (Exception ex) {
            log.warn("Product cache preload failed err={}", ex.getMessage());
        }
    }

    private String key(Long productId) {
        return KEY_PREFIX + productId;
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
