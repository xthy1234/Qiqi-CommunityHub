package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.PointsRule;

import java.util.Map;

/**
 * 积分规则服务
 */
public interface PointsRuleService extends IService<PointsRule> {
    
    /**
     * 获取积分规则
     * @param ruleCode 规则代码
     * @return 积分规则
     */
    PointsRule getRule(String ruleCode);
    
    /**
     * 获取积分值
     * @param ruleCode 规则代码
     * @return 积分值
     */
    Integer getPoints(String ruleCode);
    
    /**
     * 获取每日上限
     * @param ruleCode 规则代码
     * @return 每日上限
     */
    Integer getDailyLimit(String ruleCode);
    
    /**
     * 获取连续签到奖励配置
     * @return 配置 Map
     */
    Map<Integer, Integer> getStreakBonusConfig();
}
