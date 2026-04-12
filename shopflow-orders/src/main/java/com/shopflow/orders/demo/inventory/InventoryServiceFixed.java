package com.shopflow.orders.demo.inventory;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * MATERIAL DIDÁCTICO — Tema 7: Modelos IA
 *
 * Versión corregida de InventoryService, generada con un modelo de razonamiento.
 * Los tres bugs de concurrencia han sido resueltos.
 *
 * Diferencias clave respecto a la versión buggy:
 * 1. ConcurrentHashMap en lugar de HashMap
 * 2. compute() para operación atómica de check-and-modify
 * 3. Sin ventana de race condition entre verificación y modificación
 *
 * Nota: en producción real, el stock debe persistirse en base de datos
 * con un UPDATE atómico (stock = stock - quantity WHERE stock >= quantity).
 * Esta implementación en memoria es válida solo para demos y tests unitarios.
 */
@Service
public class InventoryServiceFixed {

    // FIX #1: ConcurrentHashMap — thread-safe para operaciones individuales
    private final ConcurrentHashMap<String, Integer> stockCache = new ConcurrentHashMap<>();

    /**
     * Reserva stock de forma atómica.
     * FIX #2 + #3: compute() ejecuta la verificación y la modificación
     * como una operación atómica, eliminando la race condition.
     */
    public boolean reserveStock(String productId, int quantity) {
        // compute() es atómico: ningún otro hilo puede modificar la entrada
        // entre la verificación y la actualización.
        boolean[] reserved = {false};

        stockCache.compute(productId, (key, currentStock) -> {
            int stock = (currentStock == null) ? 0 : currentStock;
            if (stock >= quantity) {
                reserved[0] = true;
                return stock - quantity;
            }
            return stock; // sin cambio si no hay suficiente stock
        });

        return reserved[0];
    }

    public void addStock(String productId, int quantity) {
        // merge() también es atómico
        stockCache.merge(productId, quantity, Integer::sum);
    }

    public int getStock(String productId) {
        return stockCache.getOrDefault(productId, 0);
    }
}
