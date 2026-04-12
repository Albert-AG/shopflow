package com.shopflow.orders.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Aggregate Root.
 *
 * Business invariants are enforced here:
 * - State transitions are validated (can't ship a CANCELLED order)
 * - Items can only be added to PENDING orders
 * - The aggregate is always in a consistent state
 *
 * Semantic methods: confirm() / ship() / deliver() / cancel()
 */
public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private Money totalAmount;
    private final String discountCode;
    private final Instant createdAt;

    private Order(OrderId id, CustomerId customerId, List<OrderItem> items,
                  OrderStatus status, Money totalAmount, String discountCode, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountCode = discountCode;
        this.createdAt = createdAt;
    }

    public static Order create(CustomerId customerId, List<OrderItem> items, String discountCode) {
        if (customerId == null) throw new IllegalArgumentException("CustomerId cannot be null");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("An order must have at least one item");
        }

        Money total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.zero(), Money::add);

        return new Order(
                OrderId.generate(),
                customerId,
                items,
                OrderStatus.PENDING,
                total,
                discountCode,
                Instant.now()
        );
    }

    // ---- Semantic state transitions ----

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot confirm order in status: " + status + ". Only PENDING orders can be confirmed."
            );
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void ship() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Cannot ship order in status: " + status + ". Only CONFIRMED orders can be shipped."
            );
        }
        this.status = OrderStatus.SHIPPED;
    }

    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException(
                "Cannot deliver order in status: " + status + ". Only SHIPPED orders can be delivered."
            );
        }
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                "Cannot cancel order in status: " + status + ". Shipped and delivered orders cannot be cancelled."
            );
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // ---- Domain behavior ----

    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot add items to order in status: " + status + ". Only PENDING orders accept new items."
            );
        }
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        this.items.add(item);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.zero(), Money::add);
    }

    // ---- Read-only accessors ----

    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public List<OrderItem> items() { return List.copyOf(items); }
    public OrderStatus status() { return status; }
    public Money totalAmount() { return totalAmount; }
    public String discountCode() { return discountCode; }
    public Instant createdAt() { return createdAt; }

    public boolean isPending() { return status == OrderStatus.PENDING; }
    public boolean isCancelled() { return status == OrderStatus.CANCELLED; }
    public boolean isDelivered() { return status == OrderStatus.DELIVERED; }
}
