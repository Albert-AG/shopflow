package com.shopflow.orders.demo.inventory;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * MATERIAL DIDÁCTICO — Tema 7: Modelos IA
 *
 * Este servicio fue generado por un modelo de IA de tier rápido (económico).
 * Contiene tres bugs de concurrencia que un modelo de razonamiento habría evitado.
 *
 * Ejercicio: identifica los bugs ANTES de leer los comentarios.
 * Luego pide al modelo de razonamiento que los corrija y compara las respuestas.
 *
 * Ver InventoryServiceFixed.java para la versión correcta.
 */
@Service
public class InventoryService {

    // BUG #1: HashMap en un @Service Singleton
    // @Service es un singleton de Spring. HashMap no es thread-safe.
    // Bajo carga concurrente, puede corromperse (loop infinito, NPE, datos perdidos).
    // Solución: ConcurrentHashMap (para caché in-memory) o persistencia en BD.
    private final Map<String, Integer> stockCache = new HashMap<>();

    /**
     * Reserva stock para un pedido.
     *
     * BUG #2: Check-then-act no atómico (race condition)
     * Entre el if(hasStock) y el reduceStock(), otro hilo puede reservar el mismo stock.
     * Resultado: se vende más stock del disponible ("overselling").
     *
     * Solución: operación atómica con @Transactional + SELECT FOR UPDATE en BD,
     * o UPDATE con WHERE stock >= quantity (check atómico).
     */
    public boolean reserveStock(String productId, int quantity) {
        // Paso 1: verificar
        if (hasEnoughStock(productId, quantity)) {
            // ← VENTANA DE RACE CONDITION: otro hilo puede pasar por aquí también
            // Paso 2: reducir (ya no es seguro)
            reduceStock(productId, quantity);
            return true;
        }
        return false;
    }

    private boolean hasEnoughStock(String productId, int quantity) {
        int currentStock = stockCache.getOrDefault(productId, 0);
        return currentStock >= quantity;
    }

    private void reduceStock(String productId, int quantity) {
        // BUG #3: operación no atómica sobre HashMap no thread-safe
        // getOrDefault + put no es atómico: entre ambas operaciones, otro hilo
        // puede modificar el valor, resultando en stock incorrecto.
        // Solución con ConcurrentHashMap: compute() o computeIfPresent() (atómicos).
        int current = stockCache.getOrDefault(productId, 0);
        stockCache.put(productId, current - quantity); // no atómico
    }

    public void addStock(String productId, int quantity) {
        int current = stockCache.getOrDefault(productId, 0);
        stockCache.put(productId, current + quantity); // mismo problema
    }

    public int getStock(String productId) {
        return stockCache.getOrDefault(productId, 0);
    }
}
