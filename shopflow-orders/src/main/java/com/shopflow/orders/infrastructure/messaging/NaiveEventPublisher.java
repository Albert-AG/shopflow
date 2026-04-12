package com.shopflow.orders.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * EXERCISE STATE (T13) — Naive event publisher.
 *
 * The critical bug: KafkaTemplate.send() is called INSIDE a @Transactional method,
 * but Kafka sends are NOT part of the database transaction.
 *
 * Scenario that breaks:
 * 1. Order is saved to DB
 * 2. kafkaTemplate.send() is called → Kafka receives the message
 * 3. DB transaction ROLLS BACK (e.g., constraint violation later in the same transaction)
 * 4. Result: Order is NOT in DB, but the Kafka event IS published
 *            → Customer receives "order confirmed" email for an order that doesn't exist
 *
 * Solution: Outbox Pattern (see OutboxEventPublisher.java)
 */
@Component
public class NaiveEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NaiveEventPublisher.class);
    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public NaiveEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // BUG: Kafka send is NOT transactional with the DB transaction
    @Transactional
    public void publishOrderCreated(UUID orderId) {
        String event = """
                {"type":"ORDER_CREATED","orderId":"%s"}
                """.formatted(orderId);

        // This goes to Kafka regardless of whether the DB transaction commits
        kafkaTemplate.send(TOPIC, orderId.toString(), event);
        log.info("Published ORDER_CREATED for order: {}", orderId);

        // If anything throws AFTER this line, the DB rolls back
        // but the Kafka message is already sent → inconsistency
    }
}
