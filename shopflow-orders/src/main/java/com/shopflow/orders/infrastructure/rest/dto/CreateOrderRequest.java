package com.shopflow.orders.infrastructure.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "customerId is required")
        String customerId,

        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemRequest> items,

        String discountCode  // optional
) {
    public record OrderItemRequest(
            @NotBlank(message = "productId is required")
            String productId,

            @Positive(message = "Quantity must be positive")
            int quantity,

            @NotBlank(message = "unitPrice is required")
            String unitPrice
    ) {}
}
