package com.shopflow.orders.application.command;

import java.util.List;

/**
 * Command object for creating a new order.
 * Used by the application layer — not a REST DTO.
 */
public record CreateOrderCommand(
        String customerId,
        List<OrderItemCommand> items,
        String discountCode
) {
    public record OrderItemCommand(
            String productId,
            int quantity,
            String unitPrice
    ) {}
}
