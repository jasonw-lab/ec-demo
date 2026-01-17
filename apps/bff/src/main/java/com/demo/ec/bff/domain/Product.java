package com.demo.ec.bff.domain;

import java.math.BigDecimal;

public record Product(Long id, Long categoryId, String name, String description, String imageUrl, BigDecimal price) {}
