package com.demo.ec.alert.application.streams;

import com.demo.ec.alert.domain.AlertRaisedEvent;
import com.demo.ec.alert.domain.ObjectMapperProvider;
import com.demo.ec.alert.domain.OrderPaymentState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderPaymentTransformer implements Transformer<String, String, KeyValue<String, String>> {
    private static final Logger log = LoggerFactory.getLogger(OrderPaymentTransformer.class);

    private final String storeName;
    private final long tConfirmSeconds;
    private final long tPaySeconds;
    private final long punctuateIntervalSeconds;
    private final ObjectMapper mapper = ObjectMapperProvider.get();

    private ProcessorContext context;
    private KeyValueStore<String, String> store;

    public OrderPaymentTransformer(
        String storeName,
        long tConfirmSeconds,
        long tPaySeconds,
        long punctuateIntervalSeconds) {
        this.storeName = storeName;
        this.tConfirmSeconds = tConfirmSeconds;
        this.tPaySeconds = tPaySeconds;
        this.punctuateIntervalSeconds = punctuateIntervalSeconds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.store = (KeyValueStore<String, String>) context.getStateStore(storeName);

        context.schedule(
            Duration.ofSeconds(punctuateIntervalSeconds),
            PunctuationType.WALL_CLOCK_TIME,
            timestamp -> scanDeadlines());
    }

    private void scanDeadlines() {
        try {
            long now = Instant.now().toEpochMilli();
            KeyValueIterator<String, String> iter = store.all();
            while (iter.hasNext()) {
                KeyValue<String, String> kv = iter.next();
                String key = kv.key;
                String v = kv.value;
                OrderPaymentState s = v == null ? new OrderPaymentState() : mapper.readValue(v, OrderPaymentState.class);
                boolean changed = false;
                if (s.ruleADeadlineEpochMs != null && !s.ruleAFired && now >= s.ruleADeadlineEpochMs && s.orderConfirmedAt == null) {
                    emitAlert(key, "A", "P2", s);
                    log.info("rule A fired: orderId={} deadlineEpochMs={} nowEpochMs={}", key, s.ruleADeadlineEpochMs, now);
                    s.ruleAFired = true;
                    changed = true;
                }
                if (s.ruleBDeadlineEpochMs != null && !s.ruleBFired && now >= s.ruleBDeadlineEpochMs && s.paymentSucceededAt == null) {
                    emitAlert(key, "B", "P2", s);
                    log.info("rule B fired: orderId={} deadlineEpochMs={} nowEpochMs={}", key, s.ruleBDeadlineEpochMs, now);
                    s.ruleBFired = true;
                    changed = true;
                }
                if (changed) {
                    store.put(key, mapper.writeValueAsString(s));
                }
            }
            iter.close();
        } catch (Exception ex) {
            log.warn("failed to scan deadlines: {}", ex.toString());
        }
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        if (key == null || value == null) {
            return null;
        }
        try {
            JsonNode node = mapper.readTree(value);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : null;
            String occurredAt = node.has("occurredAt") ? node.get("occurredAt").asText() : Instant.now().toString();
            String raw = store.get(key);
            OrderPaymentState s = raw == null ? new OrderPaymentState() : mapper.readValue(raw, OrderPaymentState.class);

            if ("PaymentSucceeded".equals(eventType)) {
                s.paymentSuccessCount = s.paymentSuccessCount + 1;
                if (s.paymentSucceededAt == null) {
                    s.paymentSucceededAt = occurredAt;
                }
                if (s.paymentSuccessCount >= 2 && !s.ruleCFired) {
                    emitAlert(key, "C", "P1", s);
                    log.info("rule C fired: orderId={} paymentSuccessCount={}", key, s.paymentSuccessCount);
                    s.ruleCFired = true;
                }
                if (s.orderConfirmedAt == null && s.paymentSucceededAt != null) {
                    long deadline = parseEventTime(s.paymentSucceededAt)
                        .plusSeconds(tConfirmSeconds)
                        .toEpochMilli();
                    s.ruleADeadlineEpochMs = deadline;
                }
            } else if ("OrderConfirmed".equals(eventType)) {
                if (s.orderConfirmedAt == null) {
                    s.orderConfirmedAt = occurredAt;
                }
                if (s.paymentSucceededAt == null && s.orderConfirmedAt != null) {
                    long deadline = parseEventTime(s.orderConfirmedAt)
                        .plusSeconds(tPaySeconds)
                        .toEpochMilli();
                    s.ruleBDeadlineEpochMs = deadline;
                }
            }

            store.put(key, mapper.writeValueAsString(s));
        } catch (Exception ex) {
            log.warn("failed to process event for key={}: {}", key, ex.toString());
        }
        return null;
    }

    private void emitAlert(String orderId, String rule, String severity, OrderPaymentState s) {
        try {
            AlertRaisedEvent a = new AlertRaisedEvent();
            a.setAlertId(UUID.randomUUID().toString());
            a.setRule(rule);
            a.setSeverity(severity);
            a.setOrderId(orderId);
            a.setDetectedAt(Instant.now().toString());
            AlertRaisedEvent.Facts facts = new AlertRaisedEvent.Facts();
            facts.orderConfirmedAt = s.orderConfirmedAt;
            facts.paymentSucceededAt = s.paymentSucceededAt;
            facts.paymentSuccessCount = s.paymentSuccessCount;
            a.setFacts(facts);
            String json = mapper.writeValueAsString(a);
            context.forward(orderId, json);
        } catch (Exception ex) {
            log.warn("failed to emit alert for orderId={}: {}", orderId, ex.toString());
        }
    }

    @Override
    public void close() {}

    private Instant parseEventTime(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception ex) {
            try {
                LocalDateTime local = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return local.atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
                return Instant.now();
            }
        }
    }
}
