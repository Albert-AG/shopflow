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
 *
 * EXERCISE STATE (T10): No resilience patterns.
 * Problems:
 * - No timeout: if payments-stub delays 8s, this thread is blocked for 8s
 * - No retry: a transient 503 fails the entire order
 * - No circuit breaker: if the service is down, every request blocks
 * - No fallback: the only outcome is success or exception
 *
 * The student adds Resilience4j patterns in T10 exercise.
 * See PaymentClientResilientVersion.java for the solution.
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestClient restClient;

    public PaymentClient() {
        // No timeout configured — RestClient with defaults
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }

    /**
     * Processes a payment for an order.
     * No resilience: throws exception on any failure.
     *
     * @throws org.springframework.web.client.RestClientException on HTTP errors or timeouts
     */
    public PaymentResult processPayment(UUID orderId, BigDecimal amount) {
        log.info("Processing payment for order: {} amount: {}", orderId, amount);

        Map<String, Object> request = Map.of(
                "orderId", orderId.toString(),
                "amount", amount.toPlainString()
        );

        // Direct call — no timeout, no retry, no circuit breaker
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
