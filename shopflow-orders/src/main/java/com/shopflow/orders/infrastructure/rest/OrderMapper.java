package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.domain.model.*;
import com.shopflow.orders.infrastructure.rest.dto.CreateOrderRequest;
import com.shopflow.orders.infrastructure.rest.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between domain objects and REST DTOs.
 */
@Component
public class OrderMapper {

    public List<OrderItem> toDomain(List<CreateOrderRequest.OrderItemRequest> requests) {
        return requests.stream()
                .map(r -> new OrderItem(
                        ProductId.of(r.productId()),
                        r.quantity(),
                        Money.of(r.unitPrice())
                ))
                .toList();
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.id().value().toString(),
                order.status().name(),
                order.totalAmount().amount().toPlainString(),
                order.discountCode(),
                order.items().stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private OrderResponse.OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderResponse.OrderItemResponse(
                item.productId().value().toString(),
                item.quantity(),
                item.unitPrice().amount().toPlainString(),
                item.subtotal().amount().toPlainString()
        );
    }
}
