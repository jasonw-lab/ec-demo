package com.example.seata.at.order.domain.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    CANCELLED;

    public static OrderStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status: " + value);
    }
}
