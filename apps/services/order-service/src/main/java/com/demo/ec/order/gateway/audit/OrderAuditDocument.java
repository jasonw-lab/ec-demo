package com.demo.ec.order.gateway.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "order_audit")
public class OrderAuditDocument {
    @Id
    private String id;
    @Field("orderId")
    private String orderId;
    private String currentStatus;
    private List<String> processedEventIds = new ArrayList<>();
    private List<OrderAuditHistoryEntry> history = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public OrderAuditDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public List<String> getProcessedEventIds() {
        return processedEventIds;
    }

    public void setProcessedEventIds(List<String> processedEventIds) {
        this.processedEventIds = processedEventIds;
    }

    public List<OrderAuditHistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<OrderAuditHistoryEntry> history) {
        this.history = history;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
