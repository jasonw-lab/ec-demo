package com.demo.ec.es.model;

import java.util.List;

public record SearchResponse(
        List<ProductCard> items,
        long total,
        int page,
        int size,
        String didYouMean
) {
}
