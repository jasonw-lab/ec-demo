package com.example.seata.at.account.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seata.at.account.domain.entity.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
