package com.demo.ec.storage.gateway;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.ec.storage.domain.StorageTccFreeze;
import org.apache.ibatis.annotations.Mapper;

/**
 * Storage の TCC 凍結テーブル（t_storage_tcc_freeze）に対する MyBatis-Plus Mapper。
 * - xid 単位の凍結レコードへアクセスします。
 * - 冪等性／サスペンション対策／空振り回避のための状態管理に利用されます。
 */
@Mapper
public interface StorageTccFreezeMapper extends BaseMapper<StorageTccFreeze> {
}
