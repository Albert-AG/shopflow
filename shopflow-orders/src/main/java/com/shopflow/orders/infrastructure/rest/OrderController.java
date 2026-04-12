package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.domain.model.*;
import com.shopflow.orders.domain.port.out.OrderRepository;
import com.shopflow.orders.infrastructure.rest.dto.CreateOrderRequest;
import com.shopflow.orders.infrastructure.rest.dto.OrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(
                        ProductId.of(i.productId()),
                        i.quantity(),
                        Money.of(i.unitPrice())
                ))
                .toList();

        Order order = Order.create(CustomerId.of(request.customerId()), items, request.discountCode());
        orderRepository.save(order);

        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + order.id()))
                .body(toResponse(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        return orderRepository.findById(OrderId.of(id))
                .map(order -> ResponseEntity.ok(toResponse(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<OrderResponse> orders = orderRepository.findAll(page, size).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(orders);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.items().stream()
                .map(i -> new OrderResponse.OrderItemResponse(
                        i.productId().toString(),
                        i.quantity(),
                        i.unitPrice().amount().toPlainString(),
                        i.subtotal().amount().toPlainString()
                ))
                .toList();

        return new OrderResponse(
                order.id().toString(),
                order.customerId().toString(),
                order.status().name(),
                itemResponses,
                order.totalAmount().amount().toPlainString(),
                order.discountCode(),
                order.createdAt().toString()
        );
    }
}
