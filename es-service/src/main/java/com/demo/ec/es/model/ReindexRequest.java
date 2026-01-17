package com.demo.ec.es.model;

import jakarta.validation.constraints.NotBlank;

public record ReindexRequest(
        @NotBlank String sourceIndex,
        @NotBlank String targetIndex,
        String alias
) {
}
