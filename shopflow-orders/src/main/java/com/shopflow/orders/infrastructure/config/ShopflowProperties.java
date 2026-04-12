package com.shopflow.orders.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for ShopFlow orders module.
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
