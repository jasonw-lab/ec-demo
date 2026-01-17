package com.demo.ec.bff.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Lenient deserializer that accepts numeric values or strings like "auction-1"
 * and extracts digits to parse into a Long.
 */
public class StringToLongDeserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() != null && p.getCurrentToken().isNumeric()) {
            return p.getLongValue();
        }
        String text = p.getValueAsString();
        if (text == null) return null;
        text = text.trim();
        if (text.isEmpty()) return null;

        // Remove all non-digit characters
        String digits = text.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return (Long) ctxt.handleWeirdStringValue(Long.class, text, "Cannot parse Long from '%s'", text);
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return (Long) ctxt.handleWeirdStringValue(Long.class, text, "Digits extracted '%s' cannot be parsed as Long", digits);
        }
    }
}
