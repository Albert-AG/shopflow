package com.shopflow.orders.domain;

import com.shopflow.orders.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CancellationPolicy — T17 solution.
 * Generated with Agent Mode after finding the bugs in the PR review.
 */
class CancellationPolicyTest {

    private CancellationPolicy policy;
    private CustomerId customerId;
    private OrderItem item;

    @BeforeEach
    void setUp() {
        policy = new CancellationPolicy();
        customerId = new CustomerId(UUID.randomUUID());
        item = new OrderItem(
                new ProductId(UUID.randomUUID()),
                1,
                new Money(new BigDecimal("10.00"), Currency.getInstance("EUR"))
        );
    }

    @Nested
    @DisplayName("PENDING orders")
    class PendingOrders {
        @Test
        @DisplayName("should always allow cancellation")
        void should_allow_cancel_pending() {
            Order order = Order.create(customerId, List.of(item), null);
            assertThat(policy.canCancel(order, Instant.now())).isTrue();
        }
    }

    @Nested
    @DisplayName("CONFIRMED orders")
    class ConfirmedOrders {
        @Test
        @DisplayName("should allow cancellation within 1 hour")
        void should_allow_cancel_within_window() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            Instant thirtyMinutesLater = order.createdAt().plus(30, ChronoUnit.MINUTES);
            assertThat(policy.canCancel(order, thirtyMinutesLater)).isTrue();
        }

        @Test
        @DisplayName("should deny cancellation after 1 hour")
        void should_deny_cancel_after_window() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            Instant twoHoursLater = order.createdAt().plus(2, ChronoUnit.HOURS);
            assertThat(policy.canCancel(order, twoHoursLater)).isFalse();
        }
    }

    @Nested
    @DisplayName("SHIPPED orders")
    class ShippedOrders {
        @Test
        @DisplayName("should never allow cancellation")
        void should_deny_cancel_shipped() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            order.ship();
            assertThat(policy.canCancel(order, Instant.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("DELIVERED orders")
    class DeliveredOrders {
        @Test
        @DisplayName("should never allow cancellation")
        void should_deny_cancel_delivered() {
            Order order = Order.create(customerId, List.of(item), null);
            order.confirm();
            order.ship();
            order.deliver();
            assertThat(policy.canCancel(order, Instant.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("CANCELLED orders")
    class CancelledOrders {
        @Test
        @DisplayName("should not allow cancelling an already cancelled order")
        void should_deny_cancel_already_cancelled() {
            Order order = Order.create(customerId, List.of(item), null);
            order.cancel();
            assertThat(policy.canCancel(order, Instant.now())).isFalse();
        }
    }
}
