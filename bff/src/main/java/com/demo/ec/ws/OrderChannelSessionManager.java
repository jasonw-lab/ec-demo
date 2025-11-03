package com.demo.ec.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderChannelSessionManager {

    private final Map<String, Map<String, WebSocketSession>> orderSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToOrder = new ConcurrentHashMap<>();

    public void register(String orderId, WebSocketSession session) {
        orderSessions
                .computeIfAbsent(orderId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        sessionToOrder.put(session.getId(), orderId);
    }

    public void remove(WebSocketSession session) {
        String sessionId = session.getId();
        String orderId = sessionToOrder.remove(sessionId);
        if (orderId != null) {
            Map<String, WebSocketSession> sessions = orderSessions.get(orderId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    orderSessions.remove(orderId);
                }
            }
        }
    }

    public Collection<WebSocketSession> findByOrderId(String orderId) {
        Map<String, WebSocketSession> sessions = orderSessions.get(orderId);
        return sessions == null ? Collections.emptyList() : sessions.values();
    }

    public String findOrderId(WebSocketSession session) {
        return sessionToOrder.get(session.getId());
    }
}
