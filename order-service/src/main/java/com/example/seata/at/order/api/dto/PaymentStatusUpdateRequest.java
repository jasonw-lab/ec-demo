package com.example.seata.at.order.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class PaymentStatusUpdateRequest {
    /**
     * Raw status reported by PayPay (COMPLETED / FAILED / CANCELED / TIMED_OUT / etc).
     */
    private String status;
    /**
     * Optional provider code (resultInfo.code 等) or mapped failure reason.
     */
    private String code;
    /**
     * Optional provider message.
     */
    private String message;
    /**
     * PayPay webhook event id (idempotency key).
     */
    private String eventId;
    /**
     * Event timestamp. Accepts ISO-8601 or epoch seconds/millis.
     */
    private String eventTime;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    @JsonIgnore
    public String normalizedStatus() {
        return status == null ? null : status.trim().toUpperCase();
    }

    @JsonIgnore
    public Optional<LocalDateTime> eventTimeAsLocalDateTime() {
        return parseEventTime(eventTime);
    }

    private static Optional<LocalDateTime> parseEventTime(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        try {
            Instant instant = Instant.parse(trimmed);
            return Optional.of(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
        } catch (DateTimeParseException ignored) {
            try {
                long epoch = Long.parseLong(trimmed);
                Instant instant = epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
                return Optional.of(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
            } catch (NumberFormatException ignored2) {
                return Optional.empty();
            }
        }
    }

    @JsonIgnore
    public boolean hasEventId() {
        return StringUtils.hasText(eventId);
    }
}
