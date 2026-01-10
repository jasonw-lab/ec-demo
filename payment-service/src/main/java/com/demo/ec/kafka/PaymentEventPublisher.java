package com.demo.ec.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * PaymentEventPublisher
 * 決済イベントを Kafka へ publish するコンポーネント
 */
@Component
public class PaymentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String paymentsEventsTopic;

    public PaymentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${ec-demo.kafka.topics.payments-events}") String paymentsEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.paymentsEventsTopic = paymentsEventsTopic;
    }

    /**
     * PaymentSucceeded イベントを publish
     *
     * @param orderId 注文ID
     * @param paymentId 決済ID
     * @param provider 決済プロバイダー (例: PayPay)
     * @param amount 金額
     * @param currency 通貨
     */
    public void publishPaymentSucceeded(
            String orderId,
            String paymentId,
            String provider,
            Double amount,
            String currency) {
        
        if (orderId == null || orderId.isBlank()) {
            log.warn("[Kafka] skip publish PaymentSucceeded (missing orderId) paymentId={}", paymentId);
            return;
        }

        String eventId = UUID.randomUUID().toString();
        String occurredAt = Instant.now().toString();

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                eventId,
                "PaymentSucceeded",
                1,
                "Payment",
                orderId,
                occurredAt,
                new PaymentSucceededEvent.PaymentDetails(
                        paymentId,
                        provider,
                        amount,
                        currency
                )
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(paymentsEventsTopic, orderId, json);
            log.info("[Kafka] published PaymentSucceeded eventId={} orderId={} paymentId={} provider={}",
                    eventId, orderId, paymentId, provider);
        } catch (Exception e) {
            log.warn("[Kafka] publish PaymentSucceeded failed orderId={} err={}", orderId, e.toString());
        }
    }

    /**
     * PaymentSucceeded イベント構造
     */
    public record PaymentSucceededEvent(
            String eventId,
            String eventType,
            int schemaVersion,
            String aggregateType,
            String aggregateId,
            String occurredAt,
            PaymentDetails payload
    ) {
        public record PaymentDetails(
                String paymentId,
                String provider,
                Double amount,
                String currency
        ) {}
    }
}
