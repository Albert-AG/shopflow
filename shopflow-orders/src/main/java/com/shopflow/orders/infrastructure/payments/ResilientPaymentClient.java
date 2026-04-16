package com.shopflow.orders.infrastructure.payments;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resilient wrapper around {@link PaymentClient}.
 *
 * <p>Exercise T10: implement the four resilience layers in {@link #processPayment}.
 * Configuration lives in {@code application.properties} — do NOT hardcode values here.
 *
 * <p>Recommended decoration order:
 * <pre>
 *   TimeLimiter
 *     └─ Retry (maxAttempts=3, backoff exponencial, solo en 503 y errores de red)
 *          └─ CircuitBreaker (abre tras fallos consecutivos)
 *               └─ delegate.processPayment(...)
 * </pre>
 *
 * <p>If everything fails → {@link #fallback} returns {@code PaymentResult("PENDING", orderId, false)}.
 */
@Component
@Primary
public class ResilientPaymentClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentClient.class);

    private final PaymentClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executorService;

    public ResilientPaymentClient(PaymentClient delegate,
                                   CircuitBreakerRegistry circuitBreakerRegistry,
                                   RetryRegistry retryRegistry,
                                   TimeLimiterRegistry timeLimiterRegistry) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment");
        this.retry = retryRegistry.retry("payment");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("payment");
        this.executorService = Executors.newCachedThreadPool();
    }

    public PaymentClient.PaymentResult processPayment(UUID orderId, BigDecimal amount) {
        // TODO: Capa 1 — TimeLimiter
        //   La llamada no puede bloquear el thread más de 2 segundos.
        //   Usa timeLimiter.executeFutureSupplier(...) pasándole un Callable<Future<T>>
        //   construido con executorService.submit(...).

        // TODO: Capa 2 — Retry
        //   Reintentar hasta 3 veces con backoff exponencial (500 ms base).
        //   Solo reintenta en HttpServerErrorException (503) y errores de red (IOException).
        //   NO reintentar en HttpClientErrorException (400, 422, etc.).
        //   Usa Retry.decorateCallable(retry, ...).

        // TODO: Capa 3 — CircuitBreaker
        //   Envuelve la llamada directa al delegate para que cada intento individual
        //   sea registrado por el circuito.
        //   Usa CircuitBreaker.decorateCallable(circuitBreaker, ...).

        // TODO: Capa 4 — Fallback
        //   Atrapa cualquier excepción (TimeoutException, CallNotPermittedException,
        //   MaxRetriesExceededException...) y devuelve fallback(orderId).

        // Implementación frágil — eliminar al implementar las cuatro capas:
        return delegate.processPayment(orderId, amount);
    }

    private PaymentClient.PaymentResult fallback(UUID orderId) {
        log.warn("Payment fallback for order {} — marked PENDING, will retry later", orderId);
        return new PaymentClient.PaymentResult("PENDING", orderId, false);
    }
}
