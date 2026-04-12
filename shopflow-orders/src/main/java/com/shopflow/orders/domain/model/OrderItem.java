package com.shopflow.orders.domain.model;

/**
 * Represents a line item within an Order.
 * Minimum quantity: 1. Unit price cannot be negative.
 *
 * --- EJERCICIO T04: Completa este record usando autocompletado ---
 * Instrucciones:
 * 1. Abre Order.java y Money.java en tabs del IDE (neighboring files).
 * 2. Usa autocompletado para completar los campos del record.
 *    El IDE debería sugerir ProductId, int, Money a partir del contexto.
 * 3. Añade el compact constructor con las validaciones necesarias.
 * 4. Añade el método subtotal() que calcula precio * cantidad.
 *
 * Criterio de éxito: OrderItemTest.java pasa sin modificaciones.
 */
public record OrderItem(
        // TODO: completa los campos usando autocompletado
        // El IDE debe sugerir ProductId, int, Money a partir de los neighboring files
) {
    // TODO: añade compact constructor con validaciones
    // TODO: añade método subtotal()
}
