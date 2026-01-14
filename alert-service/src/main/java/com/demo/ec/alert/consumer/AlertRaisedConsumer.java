package com.demo.ec.alert.consumer;

import com.demo.ec.alert.eventcontracts.AlertRaisedEvent;
import com.demo.ec.alert.eventcontracts.ObjectMapperProvider;
import com.demo.ec.alert.service.AlertProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertRaisedConsumer {
    private static final Logger log = LoggerFactory.getLogger(AlertRaisedConsumer.class);

    private final AlertProcessService alertProcessService;

    public AlertRaisedConsumer(AlertProcessService alertProcessService) {
        this.alertProcessService = alertProcessService;
    }

    @KafkaListener(topics = "${ec-demo.kafka.topics.alerts-order-payment-inconsistency}")
    public void onMessage(String payload) {
        try {
            AlertRaisedEvent event =
                ObjectMapperProvider.get().readValue(payload, AlertRaisedEvent.class);
            log.info(
                "alert received alertId={} orderId={} rule={} severity={}",
                event.getAlertId(),
                event.getOrderId(),
                event.getRule(),
                event.getSeverity());
            alertProcessService.handle(event);
        } catch (Exception ex) {
            log.warn("failed to parse alert payload, skip message: {}", payload);
        }
    }
}
