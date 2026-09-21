package com.demo.ec.bff.gateway.websocket;

import com.demo.ec.bff.gateway.client.OrderServiceClient;
import com.demo.ec.bff.gateway.client.dto.OrderSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderStatusWebSocketHandlerTest {

    private OrderServiceClient orderServiceClient;
    private OrderChannelSessionManager sessionManager;
    private OrderStatusBroadcaster broadcaster;
    private OrderStatusWebSocketHandler handler;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        orderServiceClient = mock(OrderServiceClient.class);
        sessionManager = new OrderChannelSessionManager();
        broadcaster = mock(OrderStatusBroadcaster.class);
        handler = new OrderStatusWebSocketHandler(orderServiceClient, sessionManager, broadcaster);
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
    }

    @Test
    void shouldCloseWhenOrderIdMissing() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/ws/orders?token=TOKEN"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(broadcaster, never()).sendInitialSnapshot(any(), any());
    }

    @Test
    void shouldCloseWhenOrderNotFound() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/ws/orders?orderId=ORDER-001&token=TOKEN"));
        when(orderServiceClient.getOrder("ORDER-001")).thenReturn(Optional.empty());

        handler.afterConnectionEstablished(session);

        verify(session).close(new CloseStatus(4404, "Order not found"));
    }

    @Test
    void shouldCloseWhenTokenInvalid() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/ws/orders?orderId=ORDER-002&token=INVALID"));
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-002");
        summary.setPaymentChannelToken("VALID-TOKEN");
        when(orderServiceClient.getOrder("ORDER-002")).thenReturn(Optional.of(summary));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void shouldCloseWhenTokenExpired() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/ws/orders?orderId=ORDER-003&token=TOKEN"));
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-003");
        summary.setPaymentChannelToken("TOKEN");
        summary.setPaymentChannelExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(orderServiceClient.getOrder("ORDER-003")).thenReturn(Optional.of(summary));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void shouldRegisterAndSendSnapshotOnValidConnection() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/ws/orders?orderId=ORDER-004&token=TOKEN"));
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-004");
        summary.setPaymentChannelToken("TOKEN");
        summary.setPaymentChannelExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(orderServiceClient.getOrder("ORDER-004")).thenReturn(Optional.of(summary));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
        verify(broadcaster).sendInitialSnapshot(session, summary);
        assertEquals(1, sessionManager.findByOrderId("ORDER-004").size());
    }

    @Test
    void shouldRespondToPingAndFetchLatestStatus() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/ws/orders?orderId=ORDER-005&token=TOKEN"));
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-005");
        summary.setPaymentChannelToken("TOKEN");
        summary.setPaymentChannelExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(orderServiceClient.getOrder("ORDER-005")).thenReturn(Optional.of(summary));

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("PING"));

        verify(session).sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        verify(broadcaster, org.mockito.Mockito.times(2)).sendInitialSnapshot(session, summary);
    }

    @Test
    void shouldRemoveSessionOnClose() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/ws/orders?orderId=ORDER-006&token=TOKEN"));
        OrderSummary summary = new OrderSummary();
        summary.setOrderNo("ORDER-006");
        summary.setPaymentChannelToken("TOKEN");
        summary.setPaymentChannelExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(orderServiceClient.getOrder("ORDER-006")).thenReturn(Optional.of(summary));

        handler.afterConnectionEstablished(session);
        assertEquals(1, sessionManager.findByOrderId("ORDER-006").size());

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        assertEquals(0, sessionManager.findByOrderId("ORDER-006").size());
    }
}
