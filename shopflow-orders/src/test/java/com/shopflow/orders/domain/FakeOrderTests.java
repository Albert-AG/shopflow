package com.shopflow.orders.domain;

import com.shopflow.orders.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MATERIAL DIDÁCTICO — Tema 14: Testing con IA
 *
 * Estos tests tienen 100% de cobertura en Order.
 * PERO no protegen nada: pasan incluso si borras la lógica de negocio.
 *
 * Ejercicio:
 * 1. Ejecuta estos tests → pasan.
 * 2. Borra el cuerpo del método cancel() en Order.java → los tests SIGUEN pasando.
 * 3. ¿Por qué? Los tests verifican que "no lanza excepción" y que el objeto "no es null",
 *    pero no verifican el estado resultante.
 * 4. Usa Chat para reescribir estos tests con validaciones precisas.
 *    Los nuevos tests deben FALLAR cuando se borra la lógica.
 *
 * Ver OrderTest.java para la versión correcta.
 */
class FakeOrderTests {

    private static final CustomerId CUSTOMER = new CustomerId(UUID.randomUUID());
    private static final OrderItem ITEM = new OrderItem(
            new ProductId(UUID.randomUUID()),
            2,
            new Money(new BigDecimal("10.00"), Currency.getInstance("EUR"))
    );

    @Test
    @DisplayName("Creating an order does not throw an exception")
    void createOrder_doesNotThrow() {
        // ANTIPATRÓN: verifica que no lanza, no que el resultado sea correcto
        assertDoesNotThrow(() -> Order.create(CUSTOMER, List.of(ITEM), null));
    }

    @Test
    @DisplayName("Created order is not null")
    void createOrder_notNull() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        // ANTIPATRÓN: verificar que no es null no protege ninguna invariante
        assertThat(order).isNotNull();
    }

    @Test
    @DisplayName("Total amount is positive")
    void createOrder_totalIsPositive() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        // ANTIPATRÓN: "es positivo" no verifica que sea 20.00 EUR
        // Pasaría aunque el total fuera 0.01 o 1000000
        assertThat(order.totalAmount().amount()).isPositive();
    }

    @Test
    @DisplayName("Cancel order does not throw for PENDING order")
    void cancelOrder_doesNotThrow() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        // ANTIPATRÓN: si borras cancel() este test sigue en verde
        assertDoesNotThrow(order::cancel);
    }

    @Test
    @DisplayName("Cancelled order is not null")
    void cancelOrder_orderStillExists() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        order.cancel();
        // ANTIPATRÓN: verificar que el objeto no es null después de cancelar
        // dice absolutamente nada sobre si el status es CANCELLED
        assertThat(order).isNotNull();
    }
}
