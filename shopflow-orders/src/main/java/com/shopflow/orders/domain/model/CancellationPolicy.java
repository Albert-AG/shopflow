package com.shopflow.orders.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Feature: order-cancellation-policy (T17 exercise — incomplete)
 *
 * Business rules to implement:
 * 1. PENDING orders can always be cancelled (no time limit)
 * 2. CONFIRMED orders can be cancelled within 1 hour of confirmation
 * 3. SHIPPED orders CANNOT be cancelled (missing — this is the gap to fill)
 * 4. DELIVERED orders CANNOT be cancelled
 * 5. CANCELLED orders cannot be cancelled again
 *
 * Current state: rules 1, 2, 4, 5 are partially implemented.
 * Rule 3 is missing entirely (SHIPPED case not handled).
 * Tests are absent.
 *
 * Student exercise:
 * 1. Find the 3 intentional bugs in this class.
 * 2. Complete the missing SHIPPED case.
 * 3. Write tests for all 5 rules.
 * 4. Open a PR using the team template.
 */
public class CancellationPolicy {

    // BUG #1: Duration should be 1 hour, not 24 hours
    // The business rule says "within 1 hour of confirmation"
    private static final Duration CONFIRMED_CANCELLATION_WINDOW = Duration.ofHours(24);

    /**
     * Determines if an order can be cancelled given current time.
     *
     * @param order the order to evaluate
     * @param now   current timestamp (injected for testability)
     * @return true if cancellation is allowed
     */
    public boolean canCancel(Order order, Instant now) {
        return switch (order.status()) {
            case PENDING -> true;
            case CONFIRMED -> {
                // BUG #2: comparison is inverted — should be isBefore, not isAfter
                // Current: allows cancellation only AFTER the window (always false in practice)
                Instant deadline = order.createdAt().plus(CONFIRMED_CANCELLATION_WINDOW);
                yield now.isAfter(deadline); // ← should be now.isBefore(deadline)
            }
            // BUG #3 (missing case): SHIPPED is not handled
            // Falls through to default, which returns false — accidentally correct,
            // but only because SHIPPED happens to be the missing case.
            // Adding SHIPPED explicitly with false would make the intent clear
            // and prevent future regressions when new statuses are added.
            case DELIVERED -> false;
            case CANCELLED -> false;
            // SHIPPED is missing — it should explicitly return false
            // Without it, if OrderStatus gets a new value, this switch crashes at runtime
            default -> false;
        };
    }

    /**
     * Returns a human-readable reason why cancellation is not allowed.
     * Used in error responses.
     */
    public String getCancellationDeniedReason(Order order, Instant now) {
        return switch (order.status()) {
            case PENDING -> null; // allowed, no reason needed
            case CONFIRMED -> {
                Instant deadline = order.createdAt().plus(CONFIRMED_CANCELLATION_WINDOW);
                if (now.isAfter(deadline)) {
                    yield "Cancellation window of " + CONFIRMED_CANCELLATION_WINDOW.toHours()
                            + " hour(s) has expired";
                }
                yield null; // allowed
            }
            case SHIPPED -> "Order has already been shipped";
            case DELIVERED -> "Order has already been delivered";
            case CANCELLED -> "Order is already cancelled";
        };
    }
}
