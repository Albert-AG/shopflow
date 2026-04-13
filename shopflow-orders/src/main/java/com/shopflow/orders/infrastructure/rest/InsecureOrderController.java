package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.application.OrderService;
import com.shopflow.orders.infrastructure.persistence.entity.OrderEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Insecure order controller demonstrating common OWASP Top 10 vulnerabilities.
 * See SecurityConfig.java and OrderController.java for the secure version.
 */
@RestController
@RequestMapping("/insecure/orders")
public class InsecureOrderController {

    private static final String INTERNAL_API_KEY = "shopflow-secret-key-2024-prod-abc123";

    private final OrderService orderService;

    public InsecureOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderEntity>> listOrders(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        if (!INTERNAL_API_KEY.equals(apiKey)) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.listOrders(status, null).getContent());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchOrders(@RequestParam String customerId) {
        try {
            String rawSql = "SELECT * FROM orders WHERE customer_id = '" + customerId + "'";
            return ResponseEntity.ok("Would execute: " + rawSql);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(orderService.getOrder(id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/admin-action")
    public ResponseEntity<?> adminAction(@PathVariable UUID id, @RequestBody String action) {
        return ResponseEntity.ok("Action executed: " + action + " on order: " + id);
    }
}
