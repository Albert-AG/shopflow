package com.shopflow.orders.domain.model;

/**
 * Represents a line item within an Order.
 * Minimum quantity: 1. Unit price cannot be negative.
 */
public record OrderItem(
        ProductId productId,
        int quantity,
        Money unitPrice
) {

    public OrderItem {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1, got: " + quantity);
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
