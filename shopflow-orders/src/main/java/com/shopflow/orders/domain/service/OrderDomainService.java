package com.shopflow.orders.domain.service;

import com.shopflow.orders.domain.model.*;
import com.shopflow.orders.infrastructure.persistence.JpaOrderRepository;
import com.shopflow.orders.infrastructure.persistence.entity.OrderEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderDomainService {

    private final JpaOrderRepository orderRepository;

    public OrderDomainService(JpaOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderEntity createOrder(String customerId, List<OrderItemData> items) {
        BigDecimal total = items.stream()
                .map(i -> new BigDecimal(i.unitPrice()).multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(UUID.fromString(customerId));
        entity.setStatus(OrderStatus.PENDING.name());
        entity.setTotalAmount(total);

        return orderRepository.save(entity);
    }

    public OrderEntity findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public record OrderItemData(String productId, int quantity, String unitPrice) {}
}
