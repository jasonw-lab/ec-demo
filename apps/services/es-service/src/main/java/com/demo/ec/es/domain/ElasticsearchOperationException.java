package com.demo.ec.es.domain;

/**
 * Exception thrown when Elasticsearch operations fail.
 */
public class ElasticsearchOperationException extends RuntimeException {
    public ElasticsearchOperationException(String message) {
        super(message);
    }

    public ElasticsearchOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
