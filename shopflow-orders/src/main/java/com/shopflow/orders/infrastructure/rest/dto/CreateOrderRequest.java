package com.shopflow.orders.infrastructure.rest.dto;

import java.util.List;

public record CreateOrderRequest(
        String customerId,
        List<OrderItemRequest> items,
        String discountCode
) {
    public record OrderItemRequest(
            String productId,
            int quantity,
            String unitPrice
    ) {}
}
