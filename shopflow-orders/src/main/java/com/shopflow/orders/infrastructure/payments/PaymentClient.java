package com.shopflow.orders.infrastructure.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Client for the external payment service.
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestClient restClient;

    public PaymentClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }

    public PaymentResult processPayment(UUID orderId, BigDecimal amount) {
        log.info("Processing payment for order: {} amount: {}", orderId, amount);

        Map<String, Object> request = Map.of(
                "orderId", orderId.toString(),
                "amount", amount.toPlainString()
        );

        Map<?, ?> response = restClient.post()
                .uri("/api/payments/process")
                .body(request)
                .retrieve()
                .body(Map.class);

        String status = (String) response.get("status");
        String paymentId = (String) response.get("paymentId");

        return new PaymentResult(paymentId, orderId, "APPROVED".equals(status));
    }

    public record PaymentResult(String paymentId, UUID orderId, boolean approved) {}
}
