package com.shopflow.orders.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * IMPORTANTE: Este archivo es material didáctico del Tema 1.
 * Representa el estilo de código pre-IA (circa 2018).
 * NO modificar. NO usar como referencia.
 * Su propósito es mostrar qué problemas resuelve la IA bien aplicada.
 */
@RestController
// TODO: añadir @RequestMapping("/api/v1") -- siempre lo olvidamos
public class BeforeAiExample {

    // Inyección por campo — viola el principio de inmutabilidad
    @Autowired
    private EntityManager em;

    // Estado mutable en un singleton — problema de concurrencia
    private Map<String, Object> cache = new HashMap<>();
    private int requestCount = 0;  // no thread-safe

    /**
     * Crea un pedido.
     * Actualizado: 2019-03-14 (comentario desactualizado — el método fue reescrito en 2022)
     */
    @PostMapping("/order")
    public ResponseEntity doOrder(@RequestBody Map<String, Object> data) {
        try {
            requestCount++;

            // Validación manual sin Bean Validation
            if (data == null) {
                return ResponseEntity.status(400).body("Error: data is null");
            }
            if (data.get("customerId") == null) {
                return ResponseEntity.status(400).body("Error: customerId missing");
            }
            if (data.get("items") == null) {
                return ResponseEntity.status(400).body("Error: items missing");
            }

            // SQL concatenado — vulnerabilidad SQL Injection
            String customerId = (String) data.get("customerId");
            String sql = "SELECT * FROM customers WHERE id = '" + customerId + "'";
            List<?> customers = em.createNativeQuery(sql).getResultList();

            if (customers.isEmpty()) {
                return ResponseEntity.status(404).body("Customer not found");
            }

            // Lógica de negocio directamente en el controlador
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            double total = 0;
            List<Object[]> results = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                String productId = (String) item.get("productId");

                // Otra query SQL concatenada dentro del bucle — problema N+1
                String productSql = "SELECT price, stock FROM products WHERE id = '" + productId + "'";
                List<Object[]> productResult = em.createNativeQuery(productSql).getResultList();

                if (productResult.isEmpty()) {
                    return ResponseEntity.status(404).body("Product not found: " + productId);
                }

                Object[] product = productResult.get(0);
                double price = Double.parseDouble(product[0].toString());
                int stock = Integer.parseInt(product[1].toString());
                int qty = (Integer) item.get("quantity");

                // Sin verificar stock negativo ni cantidad <= 0
                if (stock < qty) {
                    return ResponseEntity.status(409).body("Insufficient stock for " + productId);
                }

                total += price * qty;
                results.add(new Object[]{productId, qty, price});
            }

            // Cálculo de descuentos inline — debería estar en el dominio
            String discountCode = (String) data.get("discountCode");
            if (discountCode != null) {
                if (discountCode.equals("WELCOME10")) {
                    total = total * 0.90;
                } else if (discountCode.equals("SUMMER20")) {
                    total = total * 0.80;
                } else if (discountCode.equals("VIP30")) {
                    total = total * 0.70;
                } else if (discountCode.equals("FLASH50")) {
                    if (total > 100) {
                        total = total * 0.50;
                    }
                    // else: código válido pero no aplica — sin mensaje al cliente
                }
                // Cualquier otro código: se ignora silenciosamente
            }

            // Persistencia directa con EntityManager en el controlador
            String insertSql = "INSERT INTO orders (customer_id, total_amount, status, created_at) " +
                    "VALUES ('" + customerId + "', " + total + ", 'PENDING', NOW())";
            em.createNativeQuery(insertSql).executeUpdate();

            // Buscar el ID del pedido recién creado — race condition potencial
            String lastIdSql = "SELECT MAX(id) FROM orders WHERE customer_id = '" + customerId + "'";
            Object orderId = em.createNativeQuery(lastIdSql).getSingleResult();

            // Actualizar stock en el mismo controlador — debería ser dominio/infraestructura
            for (Object[] result : results) {
                String updateStockSql = "UPDATE products SET stock = stock - " + result[1] +
                        " WHERE id = '" + result[0] + "'";
                em.createNativeQuery(updateStockSql).executeUpdate();
            }

            // Respuesta con estructura inconsistente
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("total", total);
            response.put("ok", true);
            // Falta: status, items confirmados, fecha estimada de entrega

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Anti-patrón clásico: captura genérica + stack trace en consola
            e.printStackTrace();
            // Devuelve el mensaje de la excepción al cliente — exposición de detalles internos
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Obtiene todos los pedidos.
     * FIXME: esto trae todos los registros — no escala (conocido desde dic 2021, sin corregir)
     */
    @GetMapping("/orders")
    public ResponseEntity getOrders(@RequestParam(required = false) String status) {
        try {
            // Sin paginación — falla con miles de registros
            String sql = "SELECT * FROM orders";
            if (status != null) {
                // SQL Injection #2
                sql += " WHERE status = '" + status + "'";
            }

            List<?> orders = em.createNativeQuery(sql).getResultList();

            // Serialización manual en lugar de usar DTOs o proyecciones
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object obj : orders) {
                Object[] row = (Object[]) obj;
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", row[0]);
                orderMap.put("customerId", row[1]);   // datos de cliente sin anonimizar
                orderMap.put("totalAmount", row[2]);
                orderMap.put("status", row[3]);
                orderMap.put("createdAt", row[4]);
                result.add(orderMap);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al obtener pedidos");
        }
    }

    /**
     * Cancela un pedido.
     * Nota: no verifica si el pedido pertenece al cliente que hace la petición.
     */
    @DeleteMapping("/order/{id}")
    public ResponseEntity cancelOrder(@PathVariable String id) {
        // Sin autenticación ni autorización — cualquiera puede cancelar cualquier pedido
        try {
            String sql = "UPDATE orders SET status = 'CANCELLED' WHERE id = " + id; // SQL Injection #3
            int updated = em.createNativeQuery(sql).executeUpdate();

            if (updated == 0) {
                return ResponseEntity.status(404).body("Order not found");
            }

            // Sin publicar evento de cancelación
            // Sin liberar el stock reservado
            // Sin notificar al cliente

            return ResponseEntity.ok("Order cancelled");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage()); // expone stacktrace info
        }
    }

    /**
     * Método de utilidad interno. Nadie sabe bien para qué sirve ya.
     * @deprecated usarlo puede causar inconsistencias en datos (2020)
     */
    @Deprecated
    private void processData(Object obj1, Object obj2, String temp) {
        // Parámetros con nombres sin significado
        // Lógica comentada que nunca se eliminó
        // if (obj1 != null) {
        //     doSomethingWith(obj1);
        // }
        System.out.println("Processing: " + obj1 + " " + obj2); // System.out en lugar de logger
    }
}
