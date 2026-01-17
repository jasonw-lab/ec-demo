package com.demo.ec.alert.application;

import com.demo.ec.alert.domain.AlertRaisedEvent;
import com.demo.ec.alert.domain.ObjectMapperProvider;
import com.demo.ec.alert.gateway.repository.AlertJdbcRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.sql.Timestamp;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertProcessService {
    private static final Logger log = LoggerFactory.getLogger(AlertProcessService.class);
    private static final String DEFAULT_STATUS = "NEW";

    private final AlertJdbcRepository repository;

    public AlertProcessService(AlertJdbcRepository repository) {
        this.repository = repository;
    }

    public void handle(AlertRaisedEvent event) {
        if (event.getAlertId() == null || event.getOrderId() == null || event.getRule() == null) {
            log.warn(
                "skip alert due to missing required fields: alertId={}, orderId={}, rule={}",
                event.getAlertId(),
                event.getOrderId(),
                event.getRule());
            return;
        }

        Timestamp detectedAt = parseDetectedAt(event.getDetectedAt());
        String message = buildMessage(event);
        String factsJson = toFactsJson(event);

        repository.upsert(
            event.getAlertId(),
            event.getOrderId(),
            event.getRule(),
            event.getSeverity(),
            detectedAt,
            DEFAULT_STATUS,
            message,
            factsJson);
    }

    private Timestamp parseDetectedAt(String detectedAt) {
        if (detectedAt == null || detectedAt.isBlank()) {
            return Timestamp.from(Instant.now());
        }
        try {
            return Timestamp.from(Instant.parse(detectedAt));
        } catch (Exception ex) {
            log.warn("failed to parse detectedAt={}, fallback to now", detectedAt);
            return Timestamp.from(Instant.now());
        }
    }

    private String buildMessage(AlertRaisedEvent event) {
        String severity = event.getSeverity() == null ? "" : event.getSeverity();
        return "Order payment inconsistency detected (rule "
            + event.getRule()
            + ", severity "
            + severity
            + ")";
    }

    private String toFactsJson(AlertRaisedEvent event) {
        if (event.getFacts() == null) {
            return null;
        }
        try {
            return ObjectMapperProvider.get().writeValueAsString(event.getFacts());
        } catch (JsonProcessingException ex) {
            log.warn("failed to serialize facts for alertId={}", event.getAlertId());
            return null;
        }
    }
}
