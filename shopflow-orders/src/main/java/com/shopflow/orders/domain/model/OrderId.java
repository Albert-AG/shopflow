package com.shopflow.orders.domain.model;

import java.util.UUID;

/**
 * Value Object that identifies an Order uniquely.
 * Wraps UUID to give domain meaning to the identifier.
 */
public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new IllegalArgumentException("OrderId value cannot be null");
        }
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
