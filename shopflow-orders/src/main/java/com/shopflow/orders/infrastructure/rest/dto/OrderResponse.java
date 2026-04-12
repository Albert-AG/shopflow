package com.shopflow.orders.infrastructure.rest.dto;

import java.util.List;

public record OrderResponse(
        String orderId,
        String customerId,
        String status,
        List<OrderItemResponse> items,
        String totalAmount,
        String discountCode,
        String createdAt
) {
    public record OrderItemResponse(
            String productId,
            int quantity,
            String unitPrice,
            String subtotal
    ) {}
}
