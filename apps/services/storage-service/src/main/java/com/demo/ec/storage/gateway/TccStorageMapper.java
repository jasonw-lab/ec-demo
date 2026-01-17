package com.demo.ec.storage.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.storage.domain.TccStorage;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC Storage Mapper
 */
@Mapper
public interface TccStorageMapper extends BaseMapper<TccStorage> {
}

