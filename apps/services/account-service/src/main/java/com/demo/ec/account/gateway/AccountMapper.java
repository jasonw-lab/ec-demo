package com.demo.ec.account.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.account.domain.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
