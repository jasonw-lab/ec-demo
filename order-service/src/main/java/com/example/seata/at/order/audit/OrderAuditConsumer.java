package com.example.seata.at.order.audit;

import com.example.seata.at.order.infrastructure.messaging.kafka.OrderStatusChangedEvent;
import com.example.seata.at.order.infrastructure.messaging.kafka.OrderStatusChangedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OrderAuditConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderAuditConsumer.class);
    private static final String SOURCE_SERVICE = "order-svc";

    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;

    public OrderAuditConsumer(ObjectMapper objectMapper, MongoTemplate mongoTemplate) {
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @KafkaListener(
            topics = "${ec-demo.kafka.topics.orders-events}",
            groupId = "${EC_DEMO_ORDER_AUDIT_GROUP:order-audit-consumer-group}"
    )
    public void onMessage(String raw) {
        OrderStatusChangedEvent event;
        try {
            event = objectMapper.readValue(raw, OrderStatusChangedEvent.class);
        } catch (Exception e) {
            log.warn("[OrderAudit] failed to parse event err={} raw={}", e.toString(), raw);
            return;
        }

        String eventId = event.eventId();
        if (eventId == null || eventId.isBlank()) {
            log.warn("[OrderAudit] missing eventId, skip raw={}", raw);
            return;
        }

        String eventType = event.eventType();
        if (eventType == null || !eventType.equals("OrderStatusChanged")) {
            log.info("[OrderAudit] skip non-target eventType={} eventId={}", eventType, eventId);
            return;
        }

        OrderStatusChangedPayload payload = event.payload();
        if (payload == null) {
            log.warn("[OrderAudit] missing payload eventId={}", eventId);
            return;
        }

        String orderId = payload.orderId();
        if (orderId == null || orderId.isBlank()) {
            orderId = event.aggregateId();
        }
        if (orderId == null || orderId.isBlank()) {
            log.warn("[OrderAudit] missing orderId eventId={}", eventId);
            return;
        }

        String status = payload.newStatus();
        if (status == null || status.isBlank()) {
            log.warn("[OrderAudit] missing newStatus eventId={} orderId={}", eventId, orderId);
            return;
        }

        Instant occurredAt = parseInstant(event.occurredAt());

        OrderAuditHistoryEntry historyEntry = new OrderAuditHistoryEntry();
        historyEntry.setStatus(status);
        historyEntry.setAt(occurredAt);
        historyEntry.setBy(SOURCE_SERVICE);
        historyEntry.setEventId(eventId);
        
        // reason フィールドの設定（CANCELLED などの理由を明示）
        String reason = payload.reason();
        if (reason != null && !reason.isBlank()) {
            historyEntry.setReason(reason);
        }
        
        historyEntry.setMetadata(buildMetadata(event, payload));

        Query query = new Query(Criteria.where("_id").is(orderId)
                .and("processedEventIds").ne(eventId));

        Update update = new Update()
                .addToSet("processedEventIds", eventId)
                .push("history", historyEntry)
                .set("currentStatus", status)
                .set("updatedAt", Instant.now())
                .setOnInsert("createdAt", Instant.now())
                .setOnInsert("_id", orderId);

        try {
            UpdateResult result = mongoTemplate.upsert(query, update, OrderAuditDocument.class);
            if (result.getModifiedCount() == 0 && result.getUpsertedId() == null) {
                log.info("[OrderAudit] duplicate eventId, skipping eventId={} orderId={}", eventId, orderId);
                return;
            }
            log.info("[OrderAudit] updated order audit eventId={} orderId={} status={}", eventId, orderId, status);
        } catch (DuplicateKeyException e) {
            log.info("[OrderAudit] duplicate eventId detected eventId={} orderId={}", eventId, orderId);
        } catch (Exception e) {
            log.error("[OrderAudit] failed to update order_audit eventId={} orderId={} err={}",
                    eventId, orderId, e.toString());
            throw e;
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static Map<String, Object> buildMetadata(OrderStatusChangedEvent event, OrderStatusChangedPayload payload) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfNotBlank(metadata, "sourceEvent", event.eventType());
        putIfNotBlank(metadata, "sourceService", SOURCE_SERVICE);
        putIfNotBlank(metadata, "oldStatus", payload.oldStatus());
        putIfNotBlank(metadata, "paymentStatus", payload.paymentStatus());
        putIfNotBlank(metadata, "reason", payload.reason());
        putIfNotBlank(metadata, "correlationId", event.correlationId());
        return metadata;
    }

    private static void putIfNotBlank(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }
}
