package com.demo.ec.order.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OrderDTO {
    @NotNull(message = "userId is required")
    @JsonProperty("userId")
    @JsonAlias({"user_id", "userID"})
    private Long userId;

    @NotNull(message = "productId is required")
    @JsonProperty("productId")
    @JsonAlias({"product_id", "productID"})
    private Long productId;

    @NotNull(message = "count is required")
    private Integer count;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    // Idempotent client-provided order number (unique). Optional: if missing, server will generate.
    @JsonProperty("orderNo")
    @JsonAlias({"order_no", "orderNO"})
    private String orderNo;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
}
