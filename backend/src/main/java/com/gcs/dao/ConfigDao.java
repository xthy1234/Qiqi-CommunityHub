package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.Config;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置数据访问接口
 * 提供配置相关的数据库操作
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
public interface ConfigDao extends BaseMapper<Config> {
	
    /**
     * 根据配置键名查询配置
     * @param configKey 配置键名
     * @return 配置信息
     */
    Config selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 批量插入配置
     * @param configs 配置列表
     * @return 插入结果
     */
    int insertBatch(@Param("configs") List<Config> configs);

    /**
     * 根据配置类型查询配置列表
     * @param configType 配置类型
     * @return 配置列表
     */
    List<Config> selectByConfigType(@Param("configType") String configType);

    /**
     * 获取所有启用的配置
     * @return 配置列表
     */
    List<Config> selectAllEnabled();
}
