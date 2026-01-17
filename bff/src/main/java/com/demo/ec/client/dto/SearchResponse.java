package com.demo.ec.client.dto;

import java.util.List;

public record SearchResponse(
        List<ProductCard> items,
        long total,
        int page,
        int size,
        String didYouMean
) {
}
