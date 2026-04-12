package com.shopflow.orders.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * MATERIAL DIDÁCTICO — Tema 12: DDD con IA
 *
 * Ejemplo de modelo ANÉMICO: clase con solo getters/setters,
 * sin lógica de negocio, sin invariantes.
 * La lógica de negocio "viviría" en AnemicOrderService.
 *
 * Problemas de este modelo:
 * 1. Se puede poner cualquier status sin validar la transición
 * 2. totalAmount es Double — errores de punto flotante
 * 3. La lista de items es mutable y directamente accesible
 * 4. No hay invariantes: se puede cancelar un pedido entregado
 * 5. Objetos sin semántica: setStatus("WHATEVER") compila perfectamente
 *
 * Ver Order.java para el modelo rico con invariantes y métodos semánticos.
 *
 * NO usar este código en producción.
 */
public class AnemicOrder {

    private String id;
    private String customerId;
    private String status;          // cualquier String — sin validación
    private Double totalAmount;     // Double — errores de punto flotante
    private List<Object> items = new ArrayList<>(); // mutable, tipo borrado

    // Getters y setters — sin lógica, sin protección
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; } // sin validar transición

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public List<Object> getItems() { return items; } // expone lista mutable
    public void setItems(List<Object> items) { this.items = items; }

    // "Lógica de negocio" que en el modelo anémico vive en el servicio:
    //
    // AnemicOrderService.cancelOrder(order):
    //   order.setStatus("CANCELLED");     // ¿y si ya está entregado? ningún error
    //
    // AnemicOrderService.calculateTotal(order):
    //   double total = 0;                 // se recalcula fuera del objeto
    //   for (Object item : order.getItems()) { ... }
}
