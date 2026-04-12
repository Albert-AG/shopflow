package com.shopflow.orders.domain.model;

import java.util.UUID;

/**
 * Value Object that identifies a Customer.
 */
public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId value cannot be null");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
