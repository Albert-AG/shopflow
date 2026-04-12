package com.shopflow.orders.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for ShopFlow orders module.
 *
 * Generated in T09 with:
 * "Crea una clase @ConfigurationProperties con prefix 'shopflow.orders'
 * para las propiedades de paginación y límites de negocio."
 *
 * Usage in application.properties:
 *   shopflow.orders.max-items-per-order=50
 *   shopflow.orders.default-page-size=20
 *   shopflow.orders.pending-order-ttl-hours=48
 */
@ConfigurationProperties(prefix = "shopflow.orders")
public record ShopflowProperties(
        int maxItemsPerOrder,
        int defaultPageSize,
        int pendingOrderTtlHours
) {
    public ShopflowProperties {
        if (maxItemsPerOrder <= 0) maxItemsPerOrder = 50;
        if (defaultPageSize <= 0) defaultPageSize = 20;
        if (pendingOrderTtlHours <= 0) pendingOrderTtlHours = 48;
    }
}
