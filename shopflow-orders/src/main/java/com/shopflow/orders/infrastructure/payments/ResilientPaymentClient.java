package com.shopflow.orders.infrastructure.payments;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Resilient payment client with Circuit Breaker, Retry, and Timeout.
 *
 * Generated in T10 with Chat:
 * "Añade a PaymentClient un Circuit Breaker que abra tras 3 fallos consecutivos,
 * un Retry con exponential backoff (3 intentos, 500ms base),
 * un Timeout de 2 segundos, y un método fallback que devuelva
 * PaymentResult con estado PENDING en lugar de lanzar excepción."
 *
 * Configuration is in application.properties (resilience4j.* prefix).
 *
 * @Primary makes Spring inject this instead of PaymentClient
 */
@Component
@Primary
public class ResilientPaymentClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentClient.class);
    private static final String CB_NAME = "paymentsService";

    private final RestClient restClient;

    public ResilientPaymentClient() {
        // 2-second connect + read timeout
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "fallbackPayment")
    @Retry(name = CB_NAME)
    public PaymentClient.PaymentResult processPayment(UUID orderId, BigDecimal amount) {
        log.info("Processing payment [CB: {}] order: {} amount: {}", CB_NAME, orderId, amount);

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

        log.info("Payment result: {} for order: {}", status, orderId);
        return new PaymentClient.PaymentResult(paymentId, orderId, "APPROVED".equals(status));
    }

    /**
     * Fallback: when the circuit is open or all retries are exhausted,
     * save the payment as PENDING instead of failing the order.
     * A scheduled job will retry PENDING payments later.
     */
    public PaymentClient.PaymentResult fallbackPayment(UUID orderId, BigDecimal amount, Throwable cause) {
        log.warn("Payment fallback triggered for order: {} cause: {}", orderId, cause.getMessage());
        // Return a "pending" result — the order is created but payment is deferred
        return new PaymentClient.PaymentResult("PENDING-" + orderId, orderId, false);
    }
}
