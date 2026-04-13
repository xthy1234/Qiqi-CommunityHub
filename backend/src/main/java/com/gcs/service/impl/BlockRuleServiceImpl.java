package com.gcs.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.converter.BlockRuleConverter;
import com.gcs.dao.BlockRuleDao;
import com.gcs.dto.BlockRuleCreateDTO;
import com.gcs.entity.BlockRule;
import com.gcs.service.BlockRuleService;
import com.gcs.vo.BlockRuleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BlockRuleServiceImpl extends ServiceImpl<BlockRuleDao, BlockRule> implements BlockRuleService {

    @Autowired
    private BlockRuleDao blockRuleDao;
    
    @Autowired
    private BlockRuleConverter converter;

    @Override
    @Transactional
    public String addRule(Long userId, Object dto) {
        BlockRuleCreateDTO createDTO = (BlockRuleCreateDTO) dto;
        
        // 检查是否已存在相同的规则
        QueryWrapper<BlockRule> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("rule_type", createDTO.getRuleType())
               .eq("rule_value", createDTO.getRuleValue());
        BlockRule existingRule = this.getOne(wrapper);
        
        if (existingRule != null) {
            // 如果规则已存在且未启用，则启用它
            if (!existingRule.getEnabled()) {
                existingRule.setEnabled(true);
                this.updateById(existingRule);
                return "enabled";
            }
            // 如果已经启用，返回已存在
            return "exists";
        }
        
        // 规则不存在，创建新规则
        BlockRule rule = converter.toEntity(createDTO);
        rule.setUserId(userId);
        this.save(rule);
        return "added";
    }

    @Override
    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        QueryWrapper<BlockRule> wrapper = new QueryWrapper<>();
        wrapper.eq("id", ruleId).eq("user_id", userId);
        this.remove(wrapper);
    }

    @Override
    public List<BlockRuleVO> getMyRules(Long userId) {
        QueryWrapper<BlockRule> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        return converter.toVOList(this.list(wrapper));
    }

    @Override
    @Transactional
    public void toggleRule(Long userId, Long ruleId, Boolean enabled) {
        QueryWrapper<BlockRule> wrapper = new QueryWrapper<>();
        wrapper.eq("id", ruleId).eq("user_id", userId);
        BlockRule rule = this.getOne(wrapper);
        if (rule == null) {
            throw new RuntimeException("规则不存在或无权限操作");
        }
        rule.setEnabled(enabled);
        this.updateById(rule);
    }

    @Override
    public void injectBlockConditions(Long userId, Map<String, Object> params) {
        if (userId == null) return;

        // 1. 获取屏蔽的作者 ID 列表
        List<String> blockedAuthors = blockRuleDao.selectRuleValuesByType(userId, "author");
        if (!blockedAuthors.isEmpty()) {
            params.put("blockedAuthorIds", blockedAuthors);
        }

        // 2. 获取屏蔽的分类 ID 列表
        List<String> blockedCategories = blockRuleDao.selectRuleValuesByType(userId, "category");
        if (!blockedCategories.isEmpty()) {
            params.put("blockedCategoryIds", blockedCategories);
        }
        
        // 3. 关键词屏蔽（可选，如果性能允许）
        List<String> blockedKeywords = blockRuleDao.selectRuleValuesByType(userId, "keyword");
        if (!blockedKeywords.isEmpty()) {
            params.put("blockedKeywords", blockedKeywords);
        }
    }
}
