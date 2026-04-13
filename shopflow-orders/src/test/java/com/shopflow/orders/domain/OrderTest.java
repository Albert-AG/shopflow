package com.shopflow.orders.domain;

import com.shopflow.orders.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Order domain object with precise assertions.
 */
class OrderTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private CustomerId customerId;
    private OrderItem item;

    @BeforeEach
    void setUp() {
        customerId = new CustomerId(UUID.randomUUID());
        item = new OrderItem(
                new ProductId(UUID.randomUUID()),
                2,
                new Money(new BigDecimal("10.00"), EUR)
        );
    }

    @Nested
    @DisplayName("Order creation")
    class Creation {

        @Test
        @DisplayName("should create order with PENDING status")
        void should_create_with_pending_status() {
            Order order = Order.create(customerId, List.of(item), null);
            assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("should calculate total as sum of item subtotals")
        void should_calculate_total_correctly() {
            Order order = Order.create(customerId, List.of(item), null);
            // 2 units × 10.00 EUR = 20.00 EUR
            assertThat(order.totalAmount().amount()).isEqualByComparingTo("20.00");
            assertThat(order.totalAmount().currency().getCurrencyCode()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("should fail when items list is empty")
        void should_fail_when_no_items() {
            assertThatThrownBy(() -> Order.create(customerId, List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("should fail when customerId is null")
        void should_fail_when_customer_is_null() {
            assertThatThrownBy(() -> Order.create(null, List.of(item), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition PENDING → CONFIRMED on confirm()")
        void should_confirm_pending_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should transition CONFIRMED → SHIPPED on ship()")
        void should_ship_confirmed_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            order.ship();
            assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("should transition SHIPPED → DELIVERED on deliver()")
        void should_deliver_shipped_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            order.ship();
            order.deliver();
            assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("should transition PENDING → CANCELLED on cancel()")
        void should_cancel_pending_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.cancel();
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("should fail when shipping a CANCELLED order")
        void should_not_ship_cancelled_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.cancel();
            assertThatThrownBy(order::ship)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CANCELLED");
        }


        @Test
        @DisplayName("should fail when cancelling a SHIPPED order")
        void should_not_cancel_shipped_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            order.ship();
            assertThatThrownBy(order::cancel)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SHIPPED");
        }

        @Test
        @DisplayName("should fail when cancelling a DELIVERED order")
        void should_not_cancel_delivered_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            order.ship();
            order.deliver();
            assertThatThrownBy(order::cancel)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DELIVERED");
        }
    }

    @Nested
    @DisplayName("Adding items")
    class AddingItems {

        @Test
        @DisplayName("should recalculate total when item is added")
        void should_update_total_when_item_added() {
            Order order = Order.create(customerId, List.of(item), null);
            OrderItem newItem = new OrderItem(
                    new ProductId(UUID.randomUUID()),
                    1,
                    new Money(new BigDecimal("5.00"), EUR)
            );
            order.addItem(newItem);
            // 20.00 + 5.00 = 25.00
            assertThat(order.totalAmount().amount()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("should fail when adding item to non-PENDING order")
        void should_not_add_item_to_confirmed_order() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            assertThatThrownBy(() -> order.addItem(item))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }
}
