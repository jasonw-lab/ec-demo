package com.demo.ec.order.gateway.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderStatusChangedPayload(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("userId") Long userId,
        @JsonProperty("oldStatus") String oldStatus,
        @JsonProperty("newStatus") String newStatus,
        @JsonProperty("paymentStatus") String paymentStatus,
        @JsonProperty("reason") String reason
) {}
