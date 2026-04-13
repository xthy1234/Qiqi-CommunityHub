package com.gcs.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.BlockRule;
import com.gcs.vo.BlockRuleVO;

import java.util.List;
import java.util.Map;

public interface BlockRuleService extends IService<BlockRule> {
    /**
     * 添加屏蔽规则
     * @return "added" - 新增成功, "enabled" - 已存在并重新启用, "exists" - 已存在且已启用
     */
    String addRule(Long userId, Object dto);
    void deleteRule(Long userId, Long ruleId);
    List<BlockRuleVO> getMyRules(Long userId);
    void toggleRule(Long userId, Long ruleId, Boolean enabled);
    
    /**
     * 核心方法：为查询参数注入屏蔽条件
     */
    void injectBlockConditions(Long userId, Map<String, Object> params);
}
