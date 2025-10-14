package com.example.seata.at.storage.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import jakarta.validation.constraints.NotNull;

public class DeductRequest {
    @NotNull(message = "productId is required")
    @JsonProperty("productId")
    @JsonAlias({"product_id", "productID"})
    @BusinessActionContextParameter(paramName = "productId")
    private Long productId;

    @NotNull(message = "count is required")
    @BusinessActionContextParameter(paramName = "count")
    private Integer count;

    // Optional business key for TCC orchestration
    @BusinessActionContextParameter(paramName = "orderNo")
    private String orderNo;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
}
