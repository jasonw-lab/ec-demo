package com.demo.ec.order.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.order.domain.TccOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC Order Mapper
 */
@Mapper
public interface TccOrderMapper extends BaseMapper<TccOrder> {
}

