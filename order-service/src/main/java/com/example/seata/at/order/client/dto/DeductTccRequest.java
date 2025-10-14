package com.example.seata.at.order.client.dto;

import io.seata.rm.tcc.api.BusinessActionContextParameter;


public class DeductTccRequest {

    @BusinessActionContextParameter(paramName = "orderNo")
    private String orderNo;

    @BusinessActionContextParameter(paramName = "productId")
    private Long productId;

    @BusinessActionContextParameter(paramName = "count")
    private Integer count;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
