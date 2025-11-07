package com.demo.ec.model;

import com.demo.ec.json.StringToLongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record CartItem(@JsonDeserialize(using = StringToLongDeserializer.class) Long productId, int quantity) {}
