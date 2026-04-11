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
    public void addRule(Long userId, Object dto) {
        BlockRuleCreateDTO createDTO = (BlockRuleCreateDTO) dto;
        BlockRule rule = converter.toEntity(createDTO);
        rule.setUserId(userId);
        
        // 防止重复添加
        QueryWrapper<BlockRule> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("rule_type", createDTO.getRuleType())
               .eq("rule_value", createDTO.getRuleValue());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该屏蔽规则已存在");
        }
        
        this.save(rule);
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
