package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Publishes booking state-change events to RabbitMQ.
 * Only instantiated when app.rabbitmq.enabled=true.
 */
@Service
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public BookingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a booking event asynchronously (fire-and-forget).
     * Failures are logged but never propagated to the caller.
     */
    public void publish(BookingEvent event) {
        try {
            event.setTimestamp(Instant.now());
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published {} event for bookingId={}", event.getEventType(), event.getBookingId());
        } catch (Exception ex) {
            log.error("Failed to publish {} event for bookingId={}: {}",
                    event.getEventType(), event.getBookingId(), ex.getMessage());
            // Non-blocking: report failure must never affect core booking operations
        }
    }
}

