package com.shopflow.orders.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Cancellation policy for orders.
 *
 * Business rules:
 * 1. PENDING  → always cancellable
 * 2. CONFIRMED → cancellable within 1 hour of creation
 * 3. SHIPPED  → not cancellable
 * 4. DELIVERED → not cancellable
 * 5. CANCELLED → not cancellable (already cancelled)
 */
public class CancellationPolicy {

    private static final Duration CONFIRMED_CANCELLATION_WINDOW = Duration.ofHours(1);

    public boolean canCancel(Order order, Instant now) {
        return switch (order.status()) {
            case PENDING    -> true;
            case CONFIRMED  -> {
                Instant deadline = order.createdAt().plus(CONFIRMED_CANCELLATION_WINDOW);
                yield now.isBefore(deadline);
            }
            case SHIPPED    -> false;
            case DELIVERED  -> false;
            case CANCELLED  -> false;
        };
    }

    public String getCancellationDeniedReason(Order order, Instant now) {
        return switch (order.status()) {
            case PENDING -> null;
            case CONFIRMED -> {
                Instant deadline = order.createdAt().plus(CONFIRMED_CANCELLATION_WINDOW);
                yield now.isAfter(deadline)
                        ? "Cancellation window of 1 hour has expired"
                        : null;
            }
            case SHIPPED   -> "Order has already been shipped";
            case DELIVERED -> "Order has already been delivered";
            case CANCELLED -> "Order is already cancelled";
        };
    }
}
