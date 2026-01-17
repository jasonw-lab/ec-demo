package com.demo.ec.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * Idempotent order number. Must be unique (add unique index on column t_order.order_no).
     */
    private String orderNo;
    private Long userId;
    private Long productId;
    private Integer count;
    private BigDecimal amount;
    /**
     * Order status: CREATED / PAYMENT_PENDING / PAID / CANCELLED.
     */
    private String status;
    @TableField("payment_status")
    private String paymentStatus;
    @TableField("payment_url")
    private String paymentUrl;
    @TableField("payment_requested_at")
    private LocalDateTime paymentRequestedAt;
    @TableField("payment_expires_at")
    private LocalDateTime paymentExpiresAt;
    @TableField("payment_completed_at")
    private LocalDateTime paymentCompletedAt;
    @TableField("payment_channel_token")
    private String paymentChannelToken;
    @TableField("payment_channel_expires_at")
    private LocalDateTime paymentChannelExpiresAt;
    @TableField("payment_last_event_id")
    private String paymentLastEventId;
    @TableField("fail_code")
    private String failCode;
    @TableField("fail_message")
    private String failMessage;
    @TableField("paid_at")
    private LocalDateTime paidAt;
    @TableField("failed_at")
    private LocalDateTime failedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public LocalDateTime getPaymentRequestedAt() { return paymentRequestedAt; }
    public void setPaymentRequestedAt(LocalDateTime paymentRequestedAt) { this.paymentRequestedAt = paymentRequestedAt; }
    public LocalDateTime getPaymentExpiresAt() { return paymentExpiresAt; }
    public void setPaymentExpiresAt(LocalDateTime paymentExpiresAt) { this.paymentExpiresAt = paymentExpiresAt; }
    public LocalDateTime getPaymentCompletedAt() { return paymentCompletedAt; }
    public void setPaymentCompletedAt(LocalDateTime paymentCompletedAt) { this.paymentCompletedAt = paymentCompletedAt; }
    public String getPaymentChannelToken() { return paymentChannelToken; }
    public void setPaymentChannelToken(String paymentChannelToken) { this.paymentChannelToken = paymentChannelToken; }
    public LocalDateTime getPaymentChannelExpiresAt() { return paymentChannelExpiresAt; }
    public void setPaymentChannelExpiresAt(LocalDateTime paymentChannelExpiresAt) { this.paymentChannelExpiresAt = paymentChannelExpiresAt; }
    public String getPaymentLastEventId() { return paymentLastEventId; }
    public void setPaymentLastEventId(String paymentLastEventId) { this.paymentLastEventId = paymentLastEventId; }
    public String getFailCode() { return failCode; }
    public void setFailCode(String failCode) { this.failCode = failCode; }
    public String getFailMessage() { return failMessage; }
    public void setFailMessage(String failMessage) { this.failMessage = failMessage; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
