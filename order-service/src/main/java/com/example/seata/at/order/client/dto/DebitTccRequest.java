package com.example.seata.at.order.client.dto;

import io.seata.rm.tcc.api.BusinessActionContextParameter;

import java.math.BigDecimal;

public class DebitTccRequest {
    private String orderNo;

    @BusinessActionContextParameter(paramName = "orderId")
    private Long orderId;

    @BusinessActionContextParameter(paramName = "userId")
    private Long userId;

    @BusinessActionContextParameter(paramName = "amount")
    private BigDecimal amount;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
