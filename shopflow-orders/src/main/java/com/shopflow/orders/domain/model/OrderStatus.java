package com.shopflow.orders.domain.model;

/**
 * Lifecycle states of an Order.
 * Transitions are enforced by Order domain methods (confirm, ship, deliver, cancel).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
