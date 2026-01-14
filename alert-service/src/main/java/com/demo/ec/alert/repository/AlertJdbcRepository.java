package com.demo.ec.alert.repository;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlertJdbcRepository {
    private static final String UPSERT_SQL =
        "INSERT INTO sys_pay_alert "
            + "(alert_id, order_id, rule, severity, detected_at, status, message, facts_json) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP(3)";

    private final JdbcTemplate jdbcTemplate;

    public AlertJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(
        String alertId,
        String orderId,
        String rule,
        String severity,
        Timestamp detectedAt,
        String status,
        String message,
        String factsJson) {
        jdbcTemplate.update(
            UPSERT_SQL,
            alertId,
            orderId,
            rule,
            severity,
            detectedAt,
            status,
            message,
            factsJson);
    }
}
