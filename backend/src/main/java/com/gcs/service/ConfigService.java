package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Config;
import com.gcs.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 * 提供配置相关的业务操作
 * @author 
 * @date 2026-04-16
 */
public interface ConfigService extends IService<Config> {

    /**
     * 分页查询配置列表
     *
     * @param params 查询参数
     * @param wrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Config> wrapper);

    /**
     * 根据配置键名获取配置值
     *
     * @param configKey 配置键名
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键名获取配置信息
     *
     * @param configKey 配置键名
     * @return 配置信息
     */
    Config getConfigByKey(String configKey);

    /**
     * 批量保存配置
     *
     * @param configs 配置列表
     * @return 保存结果
     */
    boolean saveConfigs(List<Config> configs);

    /**
     * 更新配置值
     *
     * @param configKey 配置键名
     * @param configValue 配置值
     * @return 更新结果
     */
    boolean updateConfigValue(String configKey, String configValue);

    /**
     * 根据配置类型获取配置列表
     *
     * @param configType 配置类型
     * @return 配置列表
     */
    List<Config> getConfigsByType(String configType);

    /**
     * 获取所有启用的配置
     *
     * @return 配置列表
     */
    List<Config> getAllEnabledConfigs();

    /**
     * 启用/禁用配置
     *
     * @param configId 配置ID
     * @param status 状态（0:禁用 1:启用）
     * @return 操作结果
     */
    boolean updateStatus(Long configId, Integer status);

    /**
     * 批量删除配置
     *
     * @param configIds 配置ID列表
     * @return 删除结果
     */
    boolean deleteConfigs(List<Long> configIds);
}
