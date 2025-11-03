package com.demo.ec.config;

import com.demo.ec.ws.OrderStatusWebSocketHandler;
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
        registry.addHandler(orderStatusWebSocketHandler, "/ws/orders")
                .setAllowedOriginPatterns("*");
    }
}
