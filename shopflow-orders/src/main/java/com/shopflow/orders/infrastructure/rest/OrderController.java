package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.domain.model.CustomerId;
import com.shopflow.orders.domain.model.Order;
import com.shopflow.orders.domain.model.OrderId;
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
    private final OrderMapper orderMapper;

    public OrderController(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = Order.create(
                CustomerId.of(request.customerId()),
                orderMapper.toDomain(request.items()),
                request.discountCode()
        );
        orderRepository.save(order);

        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + order.id()))
                .body(orderMapper.toResponse(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        return orderRepository.findById(OrderId.of(id))
                .map(order -> ResponseEntity.ok(orderMapper.toResponse(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<OrderResponse> orders = orderRepository.findAll(page, size).stream()
                .map(orderMapper::toResponse)
                .toList();
        return ResponseEntity.ok(orders);
    }
}
