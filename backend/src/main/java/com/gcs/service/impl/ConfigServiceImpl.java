package com.gcs.service.impl;

import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.enums.CommonStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.gcs.dao.ConfigDao;
import com.gcs.entity.Config;
import com.gcs.service.ConfigService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import lombok.extern.slf4j.Slf4j;

/**
 * 系统配置服务实现类
 * 提供配置相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("configService")
public class ConfigServiceImpl extends ServiceImpl<ConfigDao, Config> implements ConfigService {

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Config> wrapper) {
        validateParams(params);
        
        IPage<Config> configPage = new Query<Config>(params).getPage();
        IPage<Config> resultPage = this.page(configPage, wrapper);
        
        return new PageUtils(resultPage);
    }

    @Override
    public String getConfigValue(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new IllegalArgumentException("配置键名不能为空");
        }

        Config config = baseMapper.selectByConfigKey(configKey);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public Config getConfigByKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new IllegalArgumentException("配置键名不能为空");
        }

        return baseMapper.selectByConfigKey(configKey);
    }



    /**
     * 批量保存配置
     *
     * @param configs 配置列表
     * @return 保存结果
     */
    @Override
    public boolean saveConfigs(List<Config> configs) {
        if (CollectionUtils.isEmpty(configs)) {
            throw new IllegalArgumentException("配置列表不能为空");
        }
        
        for (Config config : configs) {
            validateConfigForSave(config);
            config.setCreateTime(LocalDateTime.now());
            if (config.getStatus() == null) {
                config.setStatus(CommonStatus.ENABLED);
            }
        }
        
        return this.saveBatch(configs);
    }

    @Override
    public boolean updateConfigValue(String configKey, String configValue) {
        if (!StringUtils.hasText(configKey)) {
            throw new IllegalArgumentException("配置键名不能为空");
        }

        Config config = getConfigByKey(configKey);
        if (config == null) {
            throw new RuntimeException("配置不存在: " + configKey);
        }

        config.setConfigValue(configValue);
        config.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(config);
    }

    @Override
    public List<Config> getConfigsByType(String configType) {
        if (!StringUtils.hasText(configType)) {
            throw new IllegalArgumentException("配置类型不能为空");
        }

        return baseMapper.selectByConfigType(configType);
    }

    @Override
    public List<Config> getAllEnabledConfigs() {
        return baseMapper.selectAllEnabled();
    }

    @Override
    public boolean updateStatus(Long configId, Integer status) {
        if (configId == null || status == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Config config = this.getById(configId);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }
        CommonStatus commonStatus = CommonStatus.valueOf(status);
        config.setStatus(commonStatus);
        config.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(config);
    }

    @Override
    public boolean deleteConfigs(List<Long> configIds) {
        if (CollectionUtils.isEmpty(configIds)) {
            throw new IllegalArgumentException("配置ID列表不能为空");
        }

        return this.removeByIds(configIds);
    }

    // ==================== 私有验证方法 ====================

    /**
     * 验证查询参数
     */
    private void validateParams(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(params)) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    /**
     * 验证配置保存参数
     */
    private void validateConfigForSave(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("配置信息不能为空");
        }
        if (!StringUtils.hasText(config.getConfigKey())) {
            throw new IllegalArgumentException("配置键名不能为空");
        }
        if (!StringUtils.hasText(config.getConfigValue())) {
            throw new IllegalArgumentException("配置值不能为空");
        }
    }
}
