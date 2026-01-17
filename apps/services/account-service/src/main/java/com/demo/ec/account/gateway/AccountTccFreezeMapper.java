package com.demo.ec.account.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.account.domain.AccountTccFreeze;
import org.apache.ibatis.annotations.Mapper;

/**
 * Account の TCC 凍結テーブル（t_account_tcc_freeze）に対する MyBatis-Plus Mapper。
 * - xid 単位の凍結レコードへアクセスします。
 * - 冪等性／サスペンション対策／空振り回避のための状態管理に利用されます。
 */
@Mapper
public interface AccountTccFreezeMapper extends BaseMapper<AccountTccFreeze> {
}
