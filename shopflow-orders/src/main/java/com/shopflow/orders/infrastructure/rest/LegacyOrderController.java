package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.domain.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * MATERIAL DIDÁCTICO — Tema 5: Copilot Chat
 *
 * Este controlador representa el estado "legacy" del módulo de pedidos:
 * lógica de negocio mezclada con la capa REST, cálculos de descuento inline,
 * sin manejo de errores estructurado y sin paginación.
 *
 * El ejercicio del alumno en T05:
 * 1. Usar Chat para detectar los problemas arquitectónicos.
 * 2. Comparar con el análisis del Tema 1.
 * 3. Pedir a la IA un plan de refactor incremental en 4 commits.
 *
 * Ver OrderController.java para la versión refactorizada.
 *
 * NO modificar este archivo directamente. Es el punto de partida del ejercicio.
 */
@RestController
@RequestMapping("/legacy/orders")
public class LegacyOrderController {

    // Lógica de acceso a datos directamente en el controlador
    @PersistenceContext
    private EntityManager entityManager;

    // Estado mutable compartido — problema de concurrencia
    private static final Map<String, Integer> discountUsageCount = new HashMap<>();

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        // Sin validación de entrada estructurada
        String customerId = (String) request.get("customerId");
        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.badRequest().body("customerId is required");
        }

        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) request.get("items");
        if (rawItems == null || rawItems.isEmpty()) {
            return ResponseEntity.badRequest().body("items cannot be empty");
        }

        // Conversión manual sin type safety
        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map<String, Object> rawItem : rawItems) {
            String productId = (String) rawItem.get("productId");
            int quantity = (Integer) rawItem.get("quantity");
            // Precio hardcodeado — en producción real esto iría a catálogo
            BigDecimal unitPrice = new BigDecimal("29.99");

            // Sin validación de stock — crea pedido aunque no haya stock
            items.add(new OrderItem(
                    ProductId.of(productId),
                    quantity,
                    Money.of(unitPrice)
            ));
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        // Lógica de descuentos inline — debería estar en el dominio
        String discountCode = (String) request.get("discountCode");
        BigDecimal finalAmount = subtotal;

        if (discountCode != null) {
            // Sin logging de uso de descuentos (posible fraude no trazado)
            switch (discountCode) {
                case "WELCOME10" -> finalAmount = subtotal.multiply(new BigDecimal("0.90"));
                case "SUMMER20" -> finalAmount = subtotal.multiply(new BigDecimal("0.80"));
                case "VIP30" -> {
                    if (subtotal.compareTo(new BigDecimal("50")) >= 0) {
                        finalAmount = subtotal.multiply(new BigDecimal("0.70"));
                    }
                    // Si el total es < 50, el descuento no aplica pero no se notifica al cliente
                }
                case "FLASH50" -> {
                    discountUsageCount.merge(discountCode, 1, Integer::sum); // no thread-safe
                    if (discountUsageCount.get(discountCode) <= 100) {
                        finalAmount = subtotal.multiply(new BigDecimal("0.50"));
                    }
                }
                // Cualquier código inválido: se ignora silenciosamente
            }
        }

        // Creación del pedido sin pasar por un caso de uso
        Order order = new Order(
                OrderId.generate(),
                CustomerId.of(customerId),
                items,
                OrderStatus.PENDING,
                Money.of(finalAmount),
                discountCode,
                java.time.Instant.now()
        );

        // Sin persistencia real en este ejemplo legacy
        // (en el código real aquí iría entityManager.persist(orderEntity))

        // Logs con información sensible (customerId en texto plano)
        System.out.println("Order created for customer: " + customerId + " amount: " + finalAmount);

        // Respuesta inconsistente — a veces devuelve el objeto completo, a veces solo el ID
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.id().toString());
        response.put("status", order.status().name());
        response.put("amount", finalAmount);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId) {

        // Sin paginación — puede devolver miles de registros
        // Sin restricción de acceso — cualquier usuario ve todos los pedidos
        // La query filtrada por customerId se construye en la capa REST, no en repositorio

        List<Map<String, Object>> mockOrders = new ArrayList<>();
        // En el código real aquí iría una query JPA sin Pageable

        return ResponseEntity.ok(mockOrders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        try {
            // Sin manejo de UUID inválido — lanza IllegalArgumentException sin contexto
            OrderId orderId = OrderId.of(id);

            // Simulación: en el código legacy aquí iría entityManager.find(...)
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            // Stack trace al cliente
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String id) {
        // Sin verificar si el pedido pertenece al cliente autenticado
        // Sin verificar si el estado permite cancelación
        // Sin publicar evento de cancelación
        // Sin liberar stock reservado

        return ResponseEntity.ok("Order " + id + " cancelled");
    }
}
