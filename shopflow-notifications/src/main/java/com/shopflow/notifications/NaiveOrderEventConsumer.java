package com.shopflow.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * EXERCISE STATE (T13) — Naive Kafka consumer with multiple problems.
 *
 * Problems for students to identify and fix:
 * 1. No error handling: if sendEmail() throws, the message is silently lost
 * 2. No Dead Letter Topic: failed messages disappear with no retry
 * 3. Default auto-commit of offsets: offset is committed BEFORE processing,
 *    so a crash during processing means the message is never reprocessed
 * 4. No idempotency check: re-delivery sends duplicate emails
 * 5. No structured logging: impossible to correlate with order traceId
 *
 * See OrderEventConsumer.java for the resilient version.
 */
@Component
public class NaiveOrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NaiveOrderEventConsumer.class);

    // BUG: auto-commit is on by default — offset committed before processing
    @KafkaListener(topics = "order-events", groupId = "notifications-group")
    public void onOrderEvent(String message) {
        log.info("Received event: {}", message); // no traceId, no orderId

        // BUG: if this throws, the message is lost — no retry, no DLQ
        sendEmail(message);
    }

    private void sendEmail(String message) {
        // Simulate sending email
        // In a real scenario this could fail (SMTP unavailable, invalid address, etc.)
        log.info("Email sent for: {}", message);
        // BUG: no idempotency — if called twice, sends two emails
    }
}
