package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.PointsTransaction;
import com.gcs.utils.PageUtils;

import java.util.Map;

/**
 * 积分服务
 */
public interface PointsService extends IService<PointsTransaction> {
    
    /**
     * 增加积分（通用方法，基于规则）
     * @param userId 用户 ID
     * @param ruleKey 规则标识（如 "post_article"）
     * @param sourceId 关联的业务 ID
     * @param description 描述
     */
    void addPoints(Long userId, String ruleKey, Long sourceId, String description);
    
    /**
     * 增加积分（自定义数值）
     * @param userId 用户 ID
     * @param amount 积分值
     * @param source 来源
     * @param sourceId 关联业务 ID
     * @param description 描述
     */
    void addPoints(Long userId, Integer amount, String source, Long sourceId, String description);
    
    /**
     * 扣除积分
     * @param userId 用户 ID
     * @param amount 积分值
     * @param source 来源
     * @param sourceId 关联业务 ID
     * @param description 描述
     */
    void deductPoints(Long userId, Integer amount, String source, Long sourceId, String description);
    
    /**
     * 获取用户当前积分
     * @param userId 用户 ID
     * @return 积分值
     */
    Integer getUserPoints(Long userId);
    
    /**
     * 分页查询积分流水
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);
}
