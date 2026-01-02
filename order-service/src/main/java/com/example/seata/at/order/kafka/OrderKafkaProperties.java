package com.example.seata.at.order.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderKafkaProperties {
    private final String ordersEventsTopic;

    public OrderKafkaProperties(@Value("${ec-demo.kafka.topics.orders-events:" + TopicNames.ORDERS_EVENTS_V1 + "}") String ordersEventsTopic) {
        this.ordersEventsTopic = ordersEventsTopic;
    }

    public String ordersEventsTopic() {
        return ordersEventsTopic;
    }
}

