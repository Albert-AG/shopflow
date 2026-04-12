package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.application.OrderService;
import com.shopflow.orders.infrastructure.persistence.entity.OrderEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * MATERIAL DIDÁCTICO — Tema 16: Seguridad con IA
 *
 * "El Código Envenenado" — Este controlador tiene 4 vulnerabilidades OWASP Top 10.
 *
 * Ejercicio:
 * 1. Sin leer el código con detenimiento, lanza el prompt de auditoría OWASP al Chat:
 *    "Actúa como auditor de seguridad especializado en OWASP Top 10.
 *     Revisa este controlador y lista las vulnerabilidades críticas."
 * 2. Anota cuántas detectó la IA. ¿Alguna falsa alarma? ¿Alguna que la IA pasó por alto?
 * 3. Usa Agent Mode para aplicar las 4 correcciones una por una.
 * 4. Verifica que mvn test sigue en verde.
 *
 * Ver SecurityConfig.java y OrderController.java para la versión segura.
 *
 * NO usar este código en producción.
 * NO modificar este archivo. Es el punto de partida del ejercicio.
 */
@RestController
@RequestMapping("/insecure/orders")
public class InsecureOrderController {

    // VULNERABILIDAD #1: Secreto hardcodeado en el código fuente
    // Cualquiera con acceso al repositorio tiene la API key.
    // Si se sube a GitHub, herramientas como GitGuardian lo detectan.
    // Solución: leer de variable de entorno o secrets manager (Vault, AWS Secrets Manager).
    private static final String INTERNAL_API_KEY = "shopflow-secret-key-2024-prod-abc123";

    private final OrderService orderService;

    public InsecureOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderEntity>> listOrders(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        // VULNERABILIDAD #1 (uso): comparación de API key en texto plano
        // Tampoco usa comparación de tiempo constante → timing attack posible
        if (!INTERNAL_API_KEY.equals(apiKey)) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.listOrders(status, null).getContent());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchOrders(@RequestParam String customerId) {
        // VULNERABILIDAD #2: SQL Injection
        // customerId se concatena directamente en la query.
        // Input: "' OR '1'='1" → devuelve todos los pedidos de todos los clientes.
        // Solución: usar @Query con parámetros nombrados (:customerId) o Spring Data method.
        try {
            String rawSql = "SELECT * FROM orders WHERE customer_id = '" + customerId + "'";
            // En un contexto real esto se ejecutaría con jdbcTemplate.queryForList(rawSql)
            // Para el ejercicio, solo mostramos el patrón vulnerable.
            return ResponseEntity.ok("Would execute: " + rawSql);
        } catch (Exception e) {
            // VULNERABILIDAD #4: Stack trace expuesto al cliente (ver abajo)
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(orderService.getOrder(id));
        } catch (Exception e) {
            // VULNERABILIDAD #4: e.getMessage() puede exponer detalles internos
            // (nombres de tablas, estructura de BD, stacktrace parcial)
            // Solución: log.error("...", e) y devolver ProblemDetail genérico
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // VULNERABILIDAD #3: Endpoint de borrado sin autorización (@PreAuthorize)
    // Cualquier usuario autenticado puede borrar cualquier pedido de cualquier cliente.
    // No hay verificación de que el pedido pertenezca al usuario que hace la petición.
    // Solución: @PreAuthorize("hasRole('ADMIN') or @orderOwnerChecker.isOwner(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/admin-action")
    public ResponseEntity<?> adminAction(@PathVariable UUID id, @RequestBody String action) {
        // VULNERABILIDAD #3 (adicional): endpoint admin sin restricción de rol
        // Cualquier usuario autenticado puede ejecutar acciones de administrador
        return ResponseEntity.ok("Action executed: " + action + " on order: " + id);
    }
}
