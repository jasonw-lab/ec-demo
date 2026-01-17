package com.demo.ec.order.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.order.domain.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
