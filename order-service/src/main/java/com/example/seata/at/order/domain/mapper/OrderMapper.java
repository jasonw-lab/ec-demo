package com.example.seata.at.order.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seata.at.order.domain.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
