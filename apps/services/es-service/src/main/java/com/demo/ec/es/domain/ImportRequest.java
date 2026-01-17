package com.demo.ec.es.domain;

import jakarta.validation.constraints.NotBlank;

public record ImportRequest(
        @NotBlank String csvPath,
        @NotBlank String imagesDir,
        Integer batchSize
) {
}
