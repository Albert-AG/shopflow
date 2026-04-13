package com.shopflow.orders.demo.inventory;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo inventory service with concurrency bugs for classroom analysis.
 */
@Service
public class InventoryService {

    private final Map<String, Integer> stockCache = new HashMap<>();

    public boolean reserveStock(String productId, int quantity) {
        if (hasEnoughStock(productId, quantity)) {
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
        int current = stockCache.getOrDefault(productId, 0);
        stockCache.put(productId, current - quantity);
    }

    public void addStock(String productId, int quantity) {
        int current = stockCache.getOrDefault(productId, 0);
        stockCache.put(productId, current + quantity); // mismo problema
    }

    public int getStock(String productId) {
        return stockCache.getOrDefault(productId, 0);
    }
}
