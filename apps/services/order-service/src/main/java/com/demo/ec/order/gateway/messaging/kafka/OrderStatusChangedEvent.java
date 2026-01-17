package com.demo.ec.order.gateway.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderStatusChangedEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("schemaVersion") int schemaVersion,
        @JsonProperty("aggregateType") String aggregateType,
        @JsonProperty("aggregateId") String aggregateId,
        @JsonProperty("occurredAt") String occurredAt,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("payload") OrderStatusChangedPayload payload
) {}
