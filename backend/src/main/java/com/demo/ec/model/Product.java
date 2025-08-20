package com.demo.ec.model;

import java.math.BigDecimal;

public record Product(Long id, Long categoryId, String name, String description, String imageUrl, BigDecimal price) {}
