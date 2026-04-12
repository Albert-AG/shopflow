package com.shopflow.orders.infrastructure.rest.dto;

import java.util.List;

public record OrderResponse(
        String orderId,
        String status,
        String totalAmount,
        String discountCode,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(
            String productId,
            int quantity,
            String unitPrice,
            String subtotal
    ) {}
}
