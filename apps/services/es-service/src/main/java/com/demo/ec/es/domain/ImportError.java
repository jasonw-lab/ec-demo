package com.demo.ec.es.domain;

public record ImportError(long lineNo, String productId, String message) {
}
