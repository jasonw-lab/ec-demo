package com.demo.ec.alert.eventcontracts;

public class AlertRaisedEvent {
    private String alertId;
    private String rule;
    private String severity;
    private String orderId;
    private String detectedAt;
    private Facts facts;

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(String detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Facts getFacts() {
        return facts;
    }

    public void setFacts(Facts facts) {
        this.facts = facts;
    }

    public static class Facts {
        public String orderConfirmedAt;
        public String paymentSucceededAt;
        public int paymentSuccessCount;

        public Facts() {}
    }
}
