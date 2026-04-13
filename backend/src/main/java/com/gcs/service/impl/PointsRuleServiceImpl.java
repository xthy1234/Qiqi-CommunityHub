package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.PointsRuleDao;
import com.gcs.entity.PointsRule;
import com.gcs.service.PointsRuleService;
import com.gcs.utils.MPUtil;
import com.gcs.utils.PageUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分规则服务实现
 */
@Slf4j
@Service("pointsRuleService")
public class PointsRuleServiceImpl extends ServiceImpl<PointsRuleDao, PointsRule> 
        implements PointsRuleService {

    @Autowired
    private PointsRuleDao pointsRuleDao;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PointsRule getRule(String ruleCode) {
        if (ruleCode == null || ruleCode.isEmpty()) {
            return null;
        }
        
        PointsRule rule = pointsRuleDao.selectByRuleCode(ruleCode);
        if (rule == null) {
            log.warn("未找到积分规则，ruleCode: {}", ruleCode);

            return createDefaultRule(ruleCode);
        }
        
        return rule;
    }

    @Override
    public Integer getPoints(String ruleCode) {
        PointsRule rule = getRule(ruleCode);
        return rule != null ? rule.getBasePoints() : getDefaultPoints(ruleCode);
    }

    @Override
    public Integer getDailyLimit(String ruleCode) {
        PointsRule rule = getRule(ruleCode);
        return rule != null ? rule.getDailyLimit() : -1;
    }

    @Override
    public Map<Integer, Integer> getStreakBonusConfig() {
        PointsRule rule = getRule("sign_in");
        if (rule != null && rule.getStreakBonus() != null) {
            try {
                return objectMapper.readValue(rule.getStreakBonus(), HashMap.class);
            } catch (Exception e) {
                log.error("解析连续奖励配置失败", e);
            }
        }
        

        Map<Integer, Integer> defaultConfig = new HashMap<>();
        defaultConfig.put(3, 5);
        defaultConfig.put(7, 20);
        return defaultConfig;
    }
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<PointsRule> wrapper = new QueryWrapper<>();
        return queryPage(params, wrapper);
    }
    
    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<PointsRule> queryWrapper) {
        // 确保 queryWrapper 是 QueryWrapper 类型
        if (!(queryWrapper instanceof QueryWrapper)) {
            queryWrapper = new QueryWrapper<>();
        }
        
        // 使用 MPUtil 处理查询条件
        queryWrapper = MPUtil.sort(MPUtil.between(MPUtil.likeOrEq((QueryWrapper<PointsRule>) queryWrapper, null), params), params);
        
        // 执行分页查询
        com.baomidou.mybatisplus.core.metadata.IPage<PointsRule> page = new com.gcs.utils.Query<PointsRule>(params).getPage();
        com.baomidou.mybatisplus.core.metadata.IPage<PointsRule> resultPage = this.page(page, queryWrapper);
        
        long totalCount = this.count(queryWrapper);
        resultPage.setTotal(totalCount);
        
        return new PageUtils(resultPage);
    }

    /**
     * 创建默认规则
     */
    private PointsRule createDefaultRule(String ruleCode) {
        PointsRule rule = new PointsRule();
        rule.setRuleCode(ruleCode);
        rule.setRuleName(getDefaultRuleName(ruleCode));
        rule.setBasePoints(getDefaultPoints(ruleCode));
        rule.setDailyLimit(-1);
        rule.setEnabled(true);
        return rule;
    }

    private String getDefaultRuleName(String ruleCode) {
        switch (ruleCode) {
            case "sign_in": return "每日签到";
            case "post_article": return "发布文章";
            case "post_comment": return "发表评论";
            case "like_received": return "获得点赞";
            case "like_given": return "给予点赞";
            case "share": return "分享文章";
            case "follow": return "关注用户";
            default: return "未知规则";
        }
    }

    private Integer getDefaultPoints(String ruleCode) {
        switch (ruleCode) {
            case "sign_in": return 10;
            case "post_article": return 20;
            case "post_comment": return 5;
            case "like_received": return 2;
            case "like_given": return 1;
            case "share": return 3;
            case "follow": return 2;
            default: return 0;
        }
    }
}
