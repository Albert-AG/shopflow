package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.application.OrderCheckoutService;
import com.shopflow.orders.application.OrderService;
import com.shopflow.orders.application.command.CreateOrderCommand;
import com.shopflow.orders.infrastructure.persistence.entity.OrderEntity;
import com.shopflow.orders.infrastructure.rest.dto.CreateOrderRequest;
import com.shopflow.orders.infrastructure.rest.dto.CreateOrderResponse;
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
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderCheckoutService orderCheckoutService;
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(OrderCheckoutService orderCheckoutService,
                           OrderService orderService,
                           OrderMapper orderMapper) {
        this.orderCheckoutService = orderCheckoutService;
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.items().stream()
                        .map(i -> new CreateOrderCommand.OrderItemCommand(
                                i.productId(), i.quantity(), i.unitPrice()))
                        .toList(),
                request.discountCode()
        );
        OrderCheckoutService.OrderCheckoutResult created = orderCheckoutService.createOrder(command);
        MDC.put("orderId", created.order().getId().toString());
        return ResponseEntity.status(201)
                .body(orderMapper.toCreateOrderResponse(created.order(), created.status(), created.paymentId()));
    }

    @GetMapping
    public Page<OrderEntity> listOrders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return orderService.listOrders(status, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderEntity> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
