package com.demo.ec.es.domain;

import java.util.List;

public record ImportResult(
        long total,
        long success,
        long failed,
        List<ImportError> errors
) {
}
