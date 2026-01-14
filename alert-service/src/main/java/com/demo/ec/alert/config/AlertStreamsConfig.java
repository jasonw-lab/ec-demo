package com.demo.ec.alert.config;

import com.demo.ec.alert.streams.OrderPaymentTransformer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertStreamsConfig {
    public static final String STORE_NAME = "order-payment-store";

    private final AlertKafkaTopicsProperties topics;
    private final AlertRulesProperties rules;

    public AlertStreamsConfig(AlertKafkaTopicsProperties topics, AlertRulesProperties rules) {
        this.topics = topics;
        this.rules = rules;
    }

    @Bean
    public KStream<String, String> topology(StreamsBuilder builder) {
        StoreBuilder<KeyValueStore<String, String>> storeBuilder =
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STORE_NAME),
                Serdes.String(),
                Serdes.String());
        builder.addStateStore(storeBuilder);

        KStream<String, String> orders = builder.stream(topics.getOrdersEvents());
        KStream<String, String> payments = builder.stream(topics.getPaymentsEvents());

        orders
            .merge(payments)
            .transform(
                () -> new OrderPaymentTransformer(
                    STORE_NAME,
                    rules.getTConfirmSeconds(),
                    rules.getTPaySeconds(),
                    rules.getPunctuateIntervalSeconds()),
                STORE_NAME)
            .to(topics.getAlertsOrderPaymentInconsistency());

        return orders;
    }
}
