package com.example.seata.at.storage.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seata.at.storage.domain.entity.TccStorage;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC Storage Mapper
 */
@Mapper
public interface TccStorageMapper extends BaseMapper<TccStorage> {
}

