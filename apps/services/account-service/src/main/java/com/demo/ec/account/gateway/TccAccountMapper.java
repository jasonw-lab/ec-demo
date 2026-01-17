package com.demo.ec.account.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.account.domain.TccAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC Account Mapper
 */
@Mapper
public interface TccAccountMapper extends BaseMapper<TccAccount> {
}

