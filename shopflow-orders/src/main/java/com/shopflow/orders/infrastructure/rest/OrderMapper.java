package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.domain.model.*;
import com.shopflow.orders.infrastructure.rest.dto.CreateOrderRequest;
import com.shopflow.orders.infrastructure.rest.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between domain objects and REST DTOs.
 *
 * ⚠️ DEMO T06 — Agent Mode: Este mapper fue generado por el agente
 * durante la demo de "discountCode propagation". Contiene dos bugs sutiles.
 * El alumno debe detectarlos durante la revisión del diff.
 *
 * Bug #1: totalAmount se mapea como null en lugar de order.totalAmount().amount().toPlainString()
 * Bug #2: Collectors.toList() devuelve una lista mutable (usar .toList() en Java 16+)
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
