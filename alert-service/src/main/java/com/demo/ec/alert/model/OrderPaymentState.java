package com.demo.ec.alert.model;

public class OrderPaymentState {
    public String orderConfirmedAt;
    public String paymentSucceededAt;
    public int paymentSuccessCount = 0;
    public Long ruleADeadlineEpochMs;
    public Long ruleBDeadlineEpochMs;
    public boolean ruleAFired = false;
    public boolean ruleBFired = false;
    public boolean ruleCFired = false;

    public OrderPaymentState() {}
}
