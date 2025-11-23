package com.demo.ec.ws;

import com.demo.ec.client.OrderServiceClient;
import com.demo.ec.client.dto.OrderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderStatusWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusWebSocketHandler.class);
    private static final CloseStatus STATUS_NOT_FOUND = new CloseStatus(4404, "Order not found");

    private final OrderServiceClient orderServiceClient;
    private final OrderChannelSessionManager sessionManager;
    private final OrderStatusBroadcaster broadcaster;

    public OrderStatusWebSocketHandler(OrderServiceClient orderServiceClient,
                                       OrderChannelSessionManager sessionManager,
                                       OrderStatusBroadcaster broadcaster) {
        this.orderServiceClient = orderServiceClient;
        this.sessionManager = sessionManager;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> params = extractQueryParams(session.getUri());
        String orderId = params.getOrDefault("orderId", params.get("order_id"));
        String token = params.getOrDefault("token", params.get("channelToken"));

        if (!StringUtils.hasText(orderId) || !StringUtils.hasText(token)) {
            log.warn("[WS] missing orderId/token. closing sessionId={} uri={}", session.getId(), session.getUri());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Optional<OrderSummary> summaryOpt = orderServiceClient.getOrder(orderId);
        if (summaryOpt.isEmpty()) {
            log.warn("[WS] order not found orderId={} sessionId={}", orderId, session.getId());
            session.close(STATUS_NOT_FOUND);
            return;
        }

        OrderSummary summary = summaryOpt.get();
        if (!token.equals(summary.getPaymentChannelToken())) {
            log.warn("[WS] invalid token for orderId={} sessionId={}", orderId, session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (summary.getPaymentChannelExpiresAt() != null
                && summary.getPaymentChannelExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[WS] channel token expired orderId={} sessionId={}", orderId, session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessionManager.register(orderId, session);
        broadcaster.sendInitialSnapshot(session, summary);
        log.info("[WS] connected orderId={} sessionId={}", orderId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String orderId = sessionManager.findOrderId(session);
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        String payload = message.getPayload();
        if ("PING".equalsIgnoreCase(payload) || "STATUS".equalsIgnoreCase(payload)) {
            orderServiceClient.getOrder(orderId).ifPresent(summary -> {
                try {
                    session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
                    broadcaster.sendInitialSnapshot(session, summary);
                } catch (Exception e) {
                    log.warn("[WS] failed to respond ping orderId={} sessionId={} err={}", orderId, session.getId(), e.getMessage());
                }
            });
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.remove(session);
        log.info("[WS] disconnected sessionId={} status={} reason={}", session.getId(), status, status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("[WS] transport error sessionId={} err={}", session.getId(), exception.getMessage());
        super.handleTransportError(session, exception);
    }

    private static Map<String, String> extractQueryParams(URI uri) {
        if (uri == null || !StringUtils.hasText(uri.getQuery())) {
            return Collections.emptyMap();
        }
        String[] pairs = uri.getQuery().split("&");
        return Arrays.stream(pairs)
                .map(pair -> pair.split("=", 2))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(
                        arr -> URLDecoder.decode(arr[0], StandardCharsets.UTF_8),
                        arr -> URLDecoder.decode(arr[1], StandardCharsets.UTF_8),
                        (a, b) -> b));
    }
}
