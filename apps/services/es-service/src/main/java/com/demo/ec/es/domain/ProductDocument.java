package com.demo.ec.es.domain;

import java.time.Instant;

public record ProductDocument(
        Long productId,
        String title,
        String description,
        Long price,
        String status,
        String thumbnailUrl,
        Instant createdAt
) {
}
