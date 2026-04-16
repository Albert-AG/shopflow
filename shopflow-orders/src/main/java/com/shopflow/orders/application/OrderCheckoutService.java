package com.shopflow.orders.application;

import com.shopflow.orders.application.command.CreateOrderCommand;
import com.shopflow.orders.domain.model.OrderStatus;
import com.shopflow.orders.infrastructure.payments.PaymentClient;
import com.shopflow.orders.infrastructure.payments.ResilientPaymentClient;
import com.shopflow.orders.infrastructure.persistence.entity.OrderEntity;
import org.springframework.stereotype.Service;

@Service
public class OrderCheckoutService {

    private final OrderService orderService;
    private final ResilientPaymentClient resilientPaymentClient;

    public OrderCheckoutService(OrderService orderService, ResilientPaymentClient resilientPaymentClient) {
        this.orderService = orderService;
        this.resilientPaymentClient = resilientPaymentClient;
    }

    public OrderCheckoutResult createOrder(CreateOrderCommand command) {
        OrderEntity createdOrder = orderService.createOrder(command);
        PaymentClient.PaymentResult paymentResult = resilientPaymentClient.processPayment(
                createdOrder.getId(),
                createdOrder.getTotalAmount()
        );
        OrderEntity updatedOrder = orderService.updatePaymentStatus(createdOrder.getId(), paymentResult);

        return new OrderCheckoutResult(
                updatedOrder,
                toPaymentStatus(paymentResult),
                paymentResult.paymentId()
        );
    }

    private String toPaymentStatus(PaymentClient.PaymentResult paymentResult) {
        if ("PENDING".equals(paymentResult.paymentId())) {
            return OrderStatus.PENDING.name();
        }
        return paymentResult.approved() ? "APPROVED" : "DECLINED";
    }

    public record OrderCheckoutResult(OrderEntity order, String status, String paymentId) {}
}
