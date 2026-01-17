package com.demo.ec.bff.domain;

import com.demo.ec.bff.config.StringToLongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record CartItem(@JsonDeserialize(using = StringToLongDeserializer.class) Long productId, int quantity) {}
