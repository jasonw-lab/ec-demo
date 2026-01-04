package com.demo.ec.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alert")
public class AlertRulesProperties {
    private long tConfirmSeconds = 30;
    private long tPaySeconds = 30;
    private long punctuateIntervalSeconds = 10;

    public long getTConfirmSeconds() {
        return tConfirmSeconds;
    }

    public void setTConfirmSeconds(long tConfirmSeconds) {
        this.tConfirmSeconds = tConfirmSeconds;
    }

    public long getTPaySeconds() {
        return tPaySeconds;
    }

    public void setTPaySeconds(long tPaySeconds) {
        this.tPaySeconds = tPaySeconds;
    }

    public long getPunctuateIntervalSeconds() {
        return punctuateIntervalSeconds;
    }

    public void setPunctuateIntervalSeconds(long punctuateIntervalSeconds) {
        this.punctuateIntervalSeconds = punctuateIntervalSeconds;
    }
}
