package com.demo.ec.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotBlank String customerName,
        @NotBlank String customerEmail,
        @Size(min = 1) @NotEmpty List<CartItem> items
) {}
