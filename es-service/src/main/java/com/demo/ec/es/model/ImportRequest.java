package com.demo.ec.es.model;

import jakarta.validation.constraints.NotBlank;

public record ImportRequest(
        @NotBlank String csvPath,
        @NotBlank String imagesDir,
        Integer batchSize
) {
}
