package com.example.seata.at.order.audit;

import com.example.seata.at.order.infrastructure.messaging.kafka.OrderStatusChangedEvent;
import com.example.seata.at.order.infrastructure.messaging.kafka.OrderStatusChangedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderAuditConsumer の単体テスト（MongoDB接続不要）
 */
class OrderAuditConsumerUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testOrderAuditHistoryEntry_WithReason() {
        // Given: reason フィールドを持つ OrderAuditHistoryEntry
        OrderAuditHistoryEntry entry = new OrderAuditHistoryEntry();
        entry.setStatus("CANCELLED");
        entry.setReason("TIMEOUT");
        entry.setEventId("evt-123");
        entry.setBy("order-svc");

        // Then: フィールドが正しく設定される
        assertEquals("CANCELLED", entry.getStatus());
        assertEquals("TIMEOUT", entry.getReason());
        assertEquals("evt-123", entry.getEventId());
        assertEquals("order-svc", entry.getBy());
    }

    @Test
    void testOrderAuditDocument_WithProcessedEventIds() {
        // Given: processedEventIds フィールドを持つ OrderAuditDocument
        OrderAuditDocument doc = new OrderAuditDocument();
        doc.setOrderId("ORD-1001");
        doc.setCurrentStatus("CANCELLED");
        doc.getProcessedEventIds().add("evt-123");
        doc.getProcessedEventIds().add("evt-456");

        // Then: フィールドが正しく設定される
        assertEquals("ORD-1001", doc.getOrderId());
        assertEquals("CANCELLED", doc.getCurrentStatus());
        assertEquals(2, doc.getProcessedEventIds().size());
        assertTrue(doc.getProcessedEventIds().contains("evt-123"));
        assertTrue(doc.getProcessedEventIds().contains("evt-456"));
    }

    @Test
    void testOrderStatusChangedPayload_Deserialization() throws Exception {
        // Given: JSON ペイロード with reason
        String json = """
            {
                "orderId": "ORD-1001",
                "userId": 100,
                "oldStatus": "PENDING",
                "newStatus": "CANCELLED",
                "paymentStatus": "FAILED",
                "reason": "TIMEOUT"
            }
            """;

        // When: デシリアライズ
        OrderStatusChangedPayload payload = objectMapper.readValue(json, OrderStatusChangedPayload.class);

        // Then: reason が正しくマッピングされる
        assertEquals("ORD-1001", payload.orderId());
        assertEquals("CANCELLED", payload.newStatus());
        assertEquals("TIMEOUT", payload.reason());
    }
}
