package com.shopflow.orders.infrastructure.rest.dto;

public record CreateOrderResponse(
        String id,
        String status,
        String paymentId,
        String totalAmount
) {
}
