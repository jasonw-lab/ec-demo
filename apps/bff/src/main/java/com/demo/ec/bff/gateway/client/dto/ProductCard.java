package com.demo.ec.bff.gateway.client.dto;

import java.time.Instant;

public record ProductCard(
        Long productId,
        String title,
        Long price,
        String thumbnailUrl,
        Instant createdAt
) {
}
