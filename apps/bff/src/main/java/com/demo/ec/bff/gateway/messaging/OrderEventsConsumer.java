package com.demo.ec.bff.gateway.messaging;

import com.demo.ec.bff.gateway.client.OrderServiceClient;
import com.demo.ec.bff.gateway.websocket.OrderStatusBroadcaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderServiceClient orderServiceClient;
    private final OrderStatusBroadcaster broadcaster;

    public OrderEventsConsumer(ObjectMapper objectMapper, OrderServiceClient orderServiceClient, OrderStatusBroadcaster broadcaster) {
        this.objectMapper = objectMapper;
        this.orderServiceClient = orderServiceClient;
        this.broadcaster = broadcaster;
    }

    @KafkaListener(topics = "${ec-demo.kafka.topics.orders-events}")
    public void onMessage(String raw) {
        // Kafka → BFF: consume order status events and trigger the existing WebSocket broadcaster.
        // Important: do not reimplement WS payload generation here; reuse the current broadcaster.
        try {
            JsonNode root = objectMapper.readTree(raw);
            String eventId = textOrNull(firstOf(root, "eventId", "event_id"));
            String eventType = textOrNull(firstOf(root, "eventType", "event_type"));
            JsonNode payload = root.path("payload");
            String orderId = textOrNull(firstOf(payload, "orderId", "order_id"));
            if (orderId == null || orderId.isBlank()) {
                log.warn("[Kafka] missing payload.orderId eventId={} eventType={} raw={}", eventId, eventType, raw);
                return;
            }
            log.info("[Kafka] consumed eventType={} eventId={} orderId={}", eventType, eventId, orderId);
            orderServiceClient.getOrder(orderId).ifPresentOrElse(
                    summary -> {
                        broadcaster.broadcast(summary);
                        // BFF → Frontend: WS notification is sent inside OrderStatusBroadcaster.
                        log.info("[Kafka] WS broadcast triggered orderId={} eventId={}", orderId, eventId);
                    },
                    () -> log.warn("[Kafka] order not found for WS broadcast orderId={} eventId={}", orderId, eventId)
            );
        } catch (Exception e) {
            log.warn("[Kafka] failed to consume order event err={} raw={}", e.toString(), raw);
        }
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static JsonNode firstOf(JsonNode parent, String... fieldNames) {
        if (parent == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode node = parent.get(fieldName);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }
}
