package com.shopflow.orders.infrastructure.payments;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Resilience tests for {@link ResilientPaymentClient}.
 *
 * <p>Uses Mockito to simulate failure modes of the external payment service.
 * In the initial skeleton state all tests FAIL — that is expected:
 * the suite describes the behavior that must be implemented.
 *
 * <p>Run before implementing:
 * <pre>mvn test -pl shopflow-orders -Dtest=PaymentClientResilienceTest</pre>
 * You will see exceptions propagating and threads blocking beyond 2 seconds.
 *
 * <p>Run after implementing each resilience layer to track progress.
 */
class PaymentClientResilienceTest {

    private PaymentClient delegate;
    private ResilientPaymentClient subject;

    @BeforeEach
    void setUp() {
        delegate = mock(PaymentClient.class);

        // Mirrors production values in application.properties — shorter waits for tests
        var cbRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(60f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());

        var retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(50))   // faster in tests; production uses 500ms
                .retryOnException(e -> e instanceof HttpServerErrorException
                        || e instanceof java.io.IOException)
                .build());

        var tlRegistry = TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))
                .build());

        subject = new ResilientPaymentClient(delegate, cbRegistry, retryRegistry, tlRegistry);
    }

    // ── Timeout ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Timeout: delegate que tarda 5s debe resolverse en menos de 2.5s con fallback PENDING")
    void should_return_PENDING_within_timeout_when_delegate_is_slow() throws Exception {
        when(delegate.processPayment(any(), any())).thenAnswer(inv -> {
            Thread.sleep(5_000); // simulates a payment service hanging
            return new PaymentClient.PaymentResult("p1", inv.getArgument(0), true);
        });

        long start = System.currentTimeMillis();
        PaymentClient.PaymentResult result = subject.processPayment(UUID.randomUUID(), BigDecimal.TEN);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed)
                .as("El TimeLimiter debe dispararse antes de 2.5 segundos")
                .isLessThan(2_500L);
        assertThat(result.paymentId())
                .as("El fallback debe devolver paymentId 'PENDING'")
                .isEqualTo("PENDING");
        assertThat(result.approved())
                .as("PENDING no es un pago aprobado")
                .isFalse();
    }

    // ── Retry ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Retry: dos 503 transitorios seguidos de un éxito deben acabar en pago aprobado")
    void should_retry_on_503_and_succeed_on_third_attempt() {
        UUID orderId = UUID.randomUUID();
        PaymentClient.PaymentResult success = new PaymentClient.PaymentResult("pay-ok", orderId, true);

        when(delegate.processPayment(any(), any()))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class)
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class)
                .thenReturn(success);

        PaymentClient.PaymentResult result = subject.processPayment(orderId, BigDecimal.TEN);

        assertThat(result.approved()).isTrue();
        assertThat(result.paymentId()).isEqualTo("pay-ok");
        verify(delegate, times(3)).processPayment(any(), any());
    }

    // ── Fallback ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Fallback: cuando se agotan los reintentos debe devolver PENDING sin lanzar excepción")
    void should_return_PENDING_when_all_retries_exhausted() {
        when(delegate.processPayment(any(), any()))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class);

        PaymentClient.PaymentResult result = subject.processPayment(UUID.randomUUID(), BigDecimal.TEN);

        assertThat(result.paymentId())
                .as("El fallback debe devolver paymentId 'PENDING'")
                .isEqualTo("PENDING");
        assertThat(result.approved()).isFalse();
        verify(delegate, times(3)).processPayment(any(), any());
    }

    // ── Circuit Breaker ──────────────────────────────────────────────────────

    @Test
    @DisplayName("CircuitBreaker: tras varios fallos las llamadas se rechazan en milisegundos sin tocar el delegate")
    void should_open_circuit_after_failures_and_reject_immediately() {
        when(delegate.processPayment(any(), any()))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class);

        // Trigger enough failures to open the circuit
        // (slidingWindowSize=5, minimumNumberOfCalls=3, failureRateThreshold=60%)
        for (int i = 0; i < 5; i++) {
            subject.processPayment(UUID.randomUUID(), BigDecimal.TEN);
        }

        int callsBeforeCircuitOpen = mockingDetails(delegate).getInvocations().size();

        // Now make the delegate healthy — circuit should block calls before they reach it
        when(delegate.processPayment(any(), any()))
                .thenReturn(new PaymentClient.PaymentResult("p1", UUID.randomUUID(), true));

        long start = System.currentTimeMillis();
        PaymentClient.PaymentResult result = subject.processPayment(UUID.randomUUID(), BigDecimal.TEN);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed)
                .as("Con el circuito abierto la llamada debe rechazarse en milisegundos")
                .isLessThan(200L);
        assertThat(result.paymentId())
                .as("El fallback del circuito abierto debe devolver PENDING")
                .isEqualTo("PENDING");
        assertThat(mockingDetails(delegate).getInvocations().size())
                .as("Con el circuito abierto no debe llegar ninguna llamada al delegate")
                .isEqualTo(callsBeforeCircuitOpen);
    }

    // ── No retry on client errors ─────────────────────────────────────────────

    @Test
    @DisplayName("No retry: un 422 no debe reintentarse — el fallback devuelve PENDING con una sola llamada")
    void should_not_retry_on_422() {
        when(delegate.processPayment(any(), any()))
                .thenThrow(HttpClientErrorException.UnprocessableEntity.class);

        PaymentClient.PaymentResult result = subject.processPayment(UUID.randomUUID(), BigDecimal.TEN);

        assertThat(result.paymentId())
                .as("El fallback debe devolver PENDING incluso en errores de cliente")
                .isEqualTo("PENDING");
        verify(delegate, times(1))
                .processPayment(any(), any()); // exactly one call — no retry for 4xx
    }
}
