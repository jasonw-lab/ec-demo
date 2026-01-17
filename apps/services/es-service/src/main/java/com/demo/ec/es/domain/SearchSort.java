package com.demo.ec.es.domain;

public enum SearchSort {
    relevance,
    newest,
    price_asc,
    price_desc;

    public static SearchSort fromString(String value) {
        if (value == null || value.isBlank()) {
            return relevance;
        }
        try {
            return SearchSort.valueOf(value.toLowerCase());
        } catch (IllegalArgumentException e) {
            return relevance;
        }
    }
}
