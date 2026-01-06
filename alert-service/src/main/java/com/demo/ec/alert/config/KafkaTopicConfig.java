package com.demo.ec.alert.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private final AlertKafkaTopicsProperties topics;

    public KafkaTopicConfig(AlertKafkaTopicsProperties topics) {
        this.topics = topics;
    }

    @Bean
    public NewTopic ordersEventsTopic() {
        return TopicBuilder.name(topics.getOrdersEvents())
            .partitions(1)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic paymentsEventsTopic() {
        return TopicBuilder.name(topics.getPaymentsEvents())
            .partitions(1)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic alertsOrderPaymentInconsistencyTopic() {
        return TopicBuilder.name(topics.getAlertsOrderPaymentInconsistency())
            .partitions(1)
            .replicas(1)
            .build();
    }
}
