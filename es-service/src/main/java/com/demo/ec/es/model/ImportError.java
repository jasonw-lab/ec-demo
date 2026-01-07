package com.demo.ec.es.model;

public record ImportError(long lineNo, String productId, String message) {
}
