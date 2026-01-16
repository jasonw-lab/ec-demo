package com.example.seata.at.storage.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StockResponse {
    @JsonProperty("productId")
    private Long productId;
    @JsonProperty("total")
    private Integer total;
    @JsonProperty("used")
    private Integer used;
    @JsonProperty("residue")
    private Integer residue;
    @JsonProperty("frozen")
    private Integer frozen;

    public StockResponse() {}

    public StockResponse(Long productId, Integer total, Integer used, Integer residue, Integer frozen) {
        this.productId = productId;
        this.total = total;
        this.used = used;
        this.residue = residue;
        this.frozen = frozen;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getUsed() { return used; }
    public void setUsed(Integer used) { this.used = used; }
    public Integer getResidue() { return residue; }
    public void setResidue(Integer residue) { this.residue = residue; }
    public Integer getFrozen() { return frozen; }
    public void setFrozen(Integer frozen) { this.frozen = frozen; }
}
