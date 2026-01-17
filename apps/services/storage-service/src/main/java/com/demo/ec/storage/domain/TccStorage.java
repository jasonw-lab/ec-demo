package com.demo.ec.storage.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TCC Storage エンティティ
 */
@Data
@TableName("tcc_storage")
public class TccStorage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String xid;
    
    private Long productId;
    
    private Integer total;
    
    private Integer used;
    
    private Integer residue;
    
    private Integer frozen;

    // 0: PENDING, 1: SUCCESS, 2: FAILED
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
