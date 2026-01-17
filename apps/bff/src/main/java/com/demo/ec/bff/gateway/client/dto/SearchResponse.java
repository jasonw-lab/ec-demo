package com.demo.ec.bff.gateway.client.dto;

import java.util.List;

public record SearchResponse(
        List<ProductCard> items,
        long total,
        int page,
        int size,
        String didYouMean
) {
}
