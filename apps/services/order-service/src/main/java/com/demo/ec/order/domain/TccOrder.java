package com.demo.ec.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TCC Order エンティティ
 */
@Data
@TableName("tcc_order")
public class TccOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String xid;
    
    private String orderNo;
    
    private Long userId;
    
    private Long productId;
    
    private Integer count;
    
    private BigDecimal amount;
    
    private String status; // PENDING, CONFIRMED, CANCELLED
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

