package com.shopflow.orders.domain.model;

/**
 * Represents a line item within an Order.
 * Minimum quantity: 1. Unit price cannot be negative.
 *
 * TODO: completa los campos, el compact constructor y el método subtotal().
 */
public record OrderItem(
        // TODO: completa los campos usando autocompletado
        // El IDE debe sugerir ProductId, int, Money a partir de los neighboring files
) {
    // TODO: añade compact constructor con validaciones
    // TODO: añade método subtotal()
}
