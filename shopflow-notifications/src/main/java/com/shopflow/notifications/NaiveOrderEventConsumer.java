package com.shopflow.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Naive Kafka consumer without error handling or idempotency.
 */
@Component
public class NaiveOrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NaiveOrderEventConsumer.class);

    @KafkaListener(topics = "order-events", groupId = "notifications-group")
    public void onOrderEvent(String message) {
        log.info("Received event: {}", message);

        sendEmail(message);
    }

    private void sendEmail(String message) {
        log.info("Email sent for: {}", message);
    }
}
