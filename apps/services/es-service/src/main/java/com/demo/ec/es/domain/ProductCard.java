package com.demo.ec.es.domain;

import java.time.Instant;

public record ProductCard(
        Long productId,
        String title,
        Long price,
        String thumbnailUrl,
        Instant createdAt
) {
}
