package com.demo.ec.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ec-demo.kafka.topics")
public class AlertKafkaTopicsProperties {
    private String ordersEvents = "ec-demo.orders.events.v1";
    private String paymentsEvents = "ec-demo.payments.events.v1";
    private String alertsOrderPaymentInconsistency = "ec-demo.alerts.order_payment_inconsistency.v1";

    public String getOrdersEvents() {
        return ordersEvents;
    }

    public void setOrdersEvents(String ordersEvents) {
        this.ordersEvents = ordersEvents;
    }

    public String getPaymentsEvents() {
        return paymentsEvents;
    }

    public void setPaymentsEvents(String paymentsEvents) {
        this.paymentsEvents = paymentsEvents;
    }

    public String getAlertsOrderPaymentInconsistency() {
        return alertsOrderPaymentInconsistency;
    }

    public void setAlertsOrderPaymentInconsistency(String alertsOrderPaymentInconsistency) {
        this.alertsOrderPaymentInconsistency = alertsOrderPaymentInconsistency;
    }
}
