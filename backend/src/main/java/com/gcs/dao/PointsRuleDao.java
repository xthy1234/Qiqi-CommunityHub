package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.PointsRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 积分规则 DAO
 */
@Mapper
public interface PointsRuleDao extends BaseMapper<PointsRule> {
    
    /**
     * 根据规则代码获取启用的规则
     */
    PointsRule selectByRuleCode(@Param("ruleCode") String ruleCode);
}
