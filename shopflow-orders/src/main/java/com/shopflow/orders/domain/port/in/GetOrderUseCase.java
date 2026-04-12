package com.shopflow.orders.domain.port.in;

import com.shopflow.orders.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetOrderUseCase {
    Order getById(UUID id);
    Page<Order> list(String status, Pageable pageable);
}
