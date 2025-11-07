package com.example.seata.at.account.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seata.at.account.domain.entity.TccAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC Account Mapper
 */
@Mapper
public interface TccAccountMapper extends BaseMapper<TccAccount> {
}

