package com.shopflow.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Simulated external payment service for T10 (Microservices — Resilience).
 *
 * Behaviors:
 * - 30% of requests: random delay of 3-8 seconds (simulates slow network)
 * - 20% of requests: returns 503 Service Unavailable (simulates downtime)
 * - 50% of requests: normal 200 response (approved or declined)
 *
 * This erratic behavior is intentional: it demonstrates why PaymentClient
 * needs Circuit Breaker + Retry + Timeout + Fallback.
 *
 * Run on port 8081: server.port=8081
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentStubController {

    private static final Logger log = LoggerFactory.getLogger(PaymentStubController.class);
    private static final Random random = new Random();

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestBody Map<String, Object> request) throws InterruptedException {

        String orderId = (String) request.getOrDefault("orderId", "unknown");
        log.info("Payment request received for order: {}", orderId);

        // 20% chance of service unavailable
        if (random.nextDouble() < 0.20) {
            log.warn("Simulating service unavailable for order: {}", orderId);
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Payment service temporarily unavailable"));
        }

        // 30% chance of slow response (simulates network latency / timeout scenario)
        if (random.nextDouble() < 0.30) {
            int delaySeconds = 3 + random.nextInt(6); // 3 to 8 seconds
            log.warn("Simulating slow response ({} seconds) for order: {}", delaySeconds, orderId);
            TimeUnit.SECONDS.sleep(delaySeconds);
        }

        // 15% chance of payment declined (normal business response)
        boolean approved = random.nextDouble() >= 0.15;

        Map<String, Object> response = Map.of(
                "paymentId", UUID.randomUUID().toString(),
                "orderId", orderId,
                "status", approved ? "APPROVED" : "DECLINED",
                "amount", request.getOrDefault("amount", "0.00"),
                "processingTimeMs", System.currentTimeMillis() % 1000
        );

        log.info("Payment {} for order: {}", approved ? "APPROVED" : "DECLINED", orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "payments-stub");
    }
}
