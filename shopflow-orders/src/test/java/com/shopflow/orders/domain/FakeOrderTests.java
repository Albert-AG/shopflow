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
 * Fake tests demonstrating common testing antipatterns.
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
        assertDoesNotThrow(() -> Order.create(CUSTOMER, List.of(ITEM), null));
    }

    @Test
    @DisplayName("Created order is not null")
    void createOrder_notNull() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        assertThat(order).isNotNull();
    }

    @Test
    @DisplayName("Total amount is positive")
    void createOrder_totalIsPositive() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        assertThat(order.totalAmount().amount()).isPositive();
    }

    @Test
    @DisplayName("Cancel order does not throw for PENDING order")
    void cancelOrder_doesNotThrow() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        assertDoesNotThrow(order::cancel);
    }

    @Test
    @DisplayName("Cancelled order is not null")
    void cancelOrder_orderStillExists() {
        Order order = Order.create(CUSTOMER, List.of(ITEM), null);
        order.cancel();
        assertThat(order).isNotNull();
    }
}
