package com.shopflow.orders.domain.port.in;

import com.shopflow.orders.domain.model.Order;

import java.util.List;
import java.util.UUID;

/**
 * Input port for reading orders.
 * No Spring Data types here — the domain must not depend on the framework.
 * Pagination is handled at the infrastructure adapter level.
 */
public interface GetOrderUseCase {
    Order getById(UUID id);
    List<Order> list(String status, int page, int size);
}
