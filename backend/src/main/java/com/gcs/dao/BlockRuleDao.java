package com.gcs.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.BlockRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlockRuleDao extends BaseMapper<BlockRule> {
    
    /**
     * 获取用户所有启用的屏蔽规则值
     */
    List<String> selectRuleValuesByType(@Param("userId") Long userId, @Param("ruleType") String ruleType);
}
