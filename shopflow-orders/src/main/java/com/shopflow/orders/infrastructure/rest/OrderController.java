package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.application.OrderService;
import com.shopflow.orders.application.command.CreateOrderCommand;
import com.shopflow.orders.infrastructure.persistence.entity.OrderEntity;
import com.shopflow.orders.infrastructure.rest.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST adapter for the Orders resource.
 *
 * EXERCISE STATE (T09): This controller is functional but missing quality layers:
 * - No @Valid on request body (invalid input returns 500 instead of 400)
 * - No GlobalExceptionHandler (exceptions leak as 500 with stack trace)
 * - No MDC / structured logging
 * - No @ConfigurationProperties (magic numbers hardcoded)
 *
 * The student adds these layers in T09 using Chat + completions.
 * See exercise/topic-09 branch.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderEntity> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.items().stream()
                        .map(i -> new CreateOrderCommand.OrderItemCommand(
                                i.productId(), i.quantity(), i.unitPrice()))
                        .toList(),
                request.discountCode()
        );
        OrderEntity created = orderService.createOrder(command);
        MDC.put("orderId", created.getId().toString());
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public Page<OrderEntity> listOrders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return orderService.listOrders(status, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable UUID id) {
        // OrderNotFoundException → 404 via GlobalExceptionHandler
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderEntity> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
