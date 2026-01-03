package com.example.seata.at.order.infrastructure.messaging.kafka;

import com.example.seata.at.order.domain.entity.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String ordersEventsTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            OrderKafkaProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.ordersEventsTopic = properties.ordersEventsTopic();
    }

    public void publishStatusChanged(Order order, String oldStatus, String newStatus, String paymentStatus, String reason, String correlationId) {
        // Best-effort publish (PoC): DB update is the source of truth.
        // If publish fails, we log and continue. Next phase: Outbox + retry/DLQ.
        String eventId = UUID.randomUUID().toString();
        String resolvedCorrelationId = correlationId == null || correlationId.isBlank() ? eventId : correlationId;

        if (order == null) {
            log.warn("[Kafka] skip publish (missing order) eventId={}", eventId);
            return;
        }

        String orderId = order.getOrderNo();
        if (orderId == null || orderId.isBlank()) {
            log.warn("[Kafka] skip publish (missing orderNo) eventId={}", eventId);
            return;
        }

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                eventId,
                "OrderStatusChanged",
                1,
                "Order",
                orderId,
                Instant.now().toString(),
                resolvedCorrelationId,
                new OrderStatusChangedPayload(
                        orderId,
                        order.getUserId(),
                        oldStatus,
                        newStatus,
                        paymentStatus,
                        reason
                )
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ordersEventsTopic, orderId, json);
            log.info("[Kafka] published eventType={} eventId={} orderId={} oldStatus={} newStatus={}",
                    event.eventType(), event.eventId(), orderId, oldStatus, newStatus);
        } catch (Exception e) {
            log.warn("[Kafka] publish failed orderId={} err={}", orderId, e.toString());
        }
    }
}
