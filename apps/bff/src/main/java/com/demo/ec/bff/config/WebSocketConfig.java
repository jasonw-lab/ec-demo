package com.demo.ec.bff.config;

import com.demo.ec.bff.gateway.websocket.OrderStatusWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderStatusWebSocketHandler orderStatusWebSocketHandler;

    public WebSocketConfig(OrderStatusWebSocketHandler orderStatusWebSocketHandler) {
        this.orderStatusWebSocketHandler = orderStatusWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // SECURITY: Restrict WebSocket origins to trusted domains only
        // This prevents Cross-Site WebSocket Hijacking (CSWSH) attacks
        // Match the same origins as HTTP CORS configuration
        registry.addHandler(orderStatusWebSocketHandler, "/ws/orders")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173"
                );
    }
}
