package com.shopflow.orders.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Naive event publisher demonstrating the dual-write problem with Kafka and database transactions.
 */
@Component
public class NaiveEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NaiveEventPublisher.class);
    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public NaiveEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void publishOrderCreated(UUID orderId) {
        String event = """
                {"type":"ORDER_CREATED","orderId":"%s"}
                """.formatted(orderId);

        kafkaTemplate.send(TOPIC, orderId.toString(), event);
        log.info("Published ORDER_CREATED for order: {}", orderId);
    }
}
