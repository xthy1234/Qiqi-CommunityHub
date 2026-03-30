package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.PointsTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分流水 DAO
 */
@Mapper
public interface PointsTransactionDao extends BaseMapper<PointsTransaction> {
}
