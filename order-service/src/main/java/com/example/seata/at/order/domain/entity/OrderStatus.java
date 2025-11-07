package com.example.seata.at.order.domain.entity;

public enum OrderStatus {
    PENDING,
    WAITING_PAYMENT,
    PAID,
    FAILED;

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
