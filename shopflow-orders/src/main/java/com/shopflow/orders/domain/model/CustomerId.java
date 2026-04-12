package com.shopflow.orders.domain.model;

import java.util.UUID;

/**
 * Value Object that identifies a Customer.
 *
 * --- EJERCICIO T04: Completa este record usando OrderId como referencia (few-shot) ---
 * Instrucciones:
 * 1. Abre OrderId.java en un tab del IDE.
 * 2. Con OrderId visible como contexto, completa este record por analogía.
 * 3. El autocompletado debería sugerir la misma estructura.
 *
 * Criterio de éxito: CustomerIdTest.java pasa sin modificaciones.
 */
public record CustomerId(UUID value) {
    // TODO: completar con autocompletado usando OrderId como neighboring file
}
