package com.demo.ec.bff.gateway.websocket;

import com.demo.ec.bff.gateway.client.dto.OrderSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;

@Component
public class OrderStatusBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusBroadcaster.class);

    private final OrderChannelSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public OrderStatusBroadcaster(OrderChannelSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    public void sendInitialSnapshot(WebSocketSession session, OrderSummary summary) {
        try {
            session.sendMessage(new TextMessage(buildPayload(summary, "SNAPSHOT")));
            log.info("[WS] snapshot sent orderId={} sessionId={}", summary.getOrderNo(), session.getId());
        } catch (IOException e) {
            log.warn("Failed to send initial snapshot orderId={} sessionId={} err={}", summary.getOrderNo(), session.getId(), e.getMessage());
        }
    }

    public void broadcast(OrderSummary summary) {
        Collection<WebSocketSession> sessions = sessionManager.findByOrderId(summary.getOrderNo());
        if (sessions.isEmpty()) {
            return;
        }
        log.info("[WS] broadcast orderId={} sessions={} orderStatus={} paymentStatus={}",
                summary.getOrderNo(), sessions.size(), summary.getStatus(), summary.getPaymentStatus());
        String payload = buildPayload(summary, "UPDATE");
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("Failed to push update orderId={} sessionId={} err={}", summary.getOrderNo(), session.getId(), e.getMessage());
            }
        }
    }

    private String buildPayload(OrderSummary summary, String eventType) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "ORDER_STATUS");
        root.put("event", eventType);
        root.put("orderId", summary.getOrderNo());
        if (summary.getAmount() != null) {
            root.put("amount", summary.getAmount());
        }
        root.put("orderStatus", summary.getStatus());
        if (summary.getPaymentStatus() != null) {
            root.put("paymentStatus", summary.getPaymentStatus());
        }
        if (summary.getPaymentUrl() != null) {
            root.put("paymentUrl", summary.getPaymentUrl());
        }
        if (summary.getPaymentExpiresAt() != null) {
            root.put("paymentExpiresAt", summary.getPaymentExpiresAt().toString());
        }
        if (summary.getPaymentCompletedAt() != null) {
            root.put("paymentCompletedAt", summary.getPaymentCompletedAt().toString());
        }
        if (summary.getPaymentChannelToken() != null) {
            root.put("channelToken", summary.getPaymentChannelToken());
        }
        if (summary.getPaymentChannelExpiresAt() != null) {
            root.put("channelTokenExpiresAt", summary.getPaymentChannelExpiresAt().toString());
        }
        root.put("result", resolveResult(summary));
        root.put("timestamp", LocalDateTime.now().toString());
        return root.toString();
    }

    private static String resolveResult(OrderSummary summary) {
        String status = summary.getStatus() == null ? "" : summary.getStatus().toUpperCase();
        return switch (status) {
            case "PAID" -> "SUCCESS";
            case "CANCELLED" -> "FAILED";
            default -> {
                String paymentStatus = summary.getPaymentStatus() == null ? "" : summary.getPaymentStatus().toUpperCase();
                if (paymentStatus.equals("TIMED_OUT") || paymentStatus.equals("EXPIRED")) {
                    yield "TIMEOUT";
                }
                yield "PENDING";
            }
        };
    }
}
