package com.example.seata.at.order.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seata.at.order.domain.entity.TccOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC Order Mapper
 */
@Mapper
public interface TccOrderMapper extends BaseMapper<TccOrder> {
}

