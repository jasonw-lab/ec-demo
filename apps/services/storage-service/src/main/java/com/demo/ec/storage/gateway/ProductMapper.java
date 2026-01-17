package com.demo.ec.storage.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.storage.domain.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
