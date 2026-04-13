package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.PointsRule;
import com.gcs.utils.PageUtils;

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
    
    /**
     * 分页查询积分规则
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);
    
    /**
     * 分页查询积分规则（带查询条件）
     * @param params 查询参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<PointsRule> queryWrapper);
}
