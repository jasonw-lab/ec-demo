package com.demo.ec.account.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.account.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}


