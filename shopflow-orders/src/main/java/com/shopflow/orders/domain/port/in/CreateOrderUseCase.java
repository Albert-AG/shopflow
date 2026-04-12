package com.shopflow.orders.domain.port.in;

import com.shopflow.orders.application.command.CreateOrderCommand;
import com.shopflow.orders.domain.model.Order;

/**
 * Input port: defines the contract for creating an order.
 * The REST adapter calls this interface — it never knows which class implements it.
 */
public interface CreateOrderUseCase {
    Order createOrder(CreateOrderCommand command);
}
