package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.Config;
import com.gcs.service.ConfigService;
import com.gcs.utils.MPUtil;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 系统配置控制器
 * 提供配置相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Slf4j
@Tag(name = "系统配置管理", description = "系统配置相关的 RESTful API 接口")
@RestController
@RequestMapping("/configs")
public class ConfigController {
    
    @Autowired
    private ConfigService configService;

    /**
     * 获取配置分页列表
     */
    @Operation(summary = "获取配置分页列表", description = "分页查询配置列表")
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "配置查询条件") Config config) {
        try {
            QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
            PageUtils page = configService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, config), params), params));
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取配置分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取配置详情
     */
    @Operation(summary = "获取配置详情", description = "根据配置 ID 获取详细信息")
    @GetMapping("/{id}")
    public R getConfigInfo(
        @Parameter(description = "配置 ID", required = true) @PathVariable("id") Long id) {
        try {
            Config config = configService.getById(id);
            if (config == null) {
                return R.error("配置不存在");
            }
            return R.ok().put("data", config);
        } catch (Exception e) {
            log.error("获取配置详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 根据配置键名获取配置信息
     */
    @Operation(summary = "根据键名获取配置", description = "根据配置键名获取配置详细信息")
    @GetMapping("/key/{configKey}")
    public R getConfigByKey(
        @Parameter(description = "配置键名", required = true) @PathVariable("configKey") String configKey) {
        try {
            Config config = configService.getConfigByKey(configKey);
            if (config == null) {
                return R.error("配置不存在");
            }
            return R.ok().put("data", config);
        } catch (Exception e) {
            log.error("根据键名获取配置失败，configKey: {}", configKey, e);
            return R.error("获取配置失败");
        }
    }

    /**
     * 根据配置键名获取配置值
     */
    @Operation(summary = "根据键名获取配置值", description = "根据配置键名获取配置值")
    @GetMapping("/key/{configKey}/value")
    public R getConfigValue(
        @Parameter(description = "配置键名", required = true) @PathVariable("configKey") String configKey) {
        try {
            String configValue = configService.getConfigValue(configKey);
            if (configValue == null) {
                return R.error("配置不存在");
            }
            return R.ok().put("data", configValue);
        } catch (Exception e) {
            log.error("根据键名获取配置值失败，configKey: {}", configKey, e);
            return R.error("获取配置值失败");
        }
    }

    /**
     * 根据配置类型获取配置列表
     */
    @Operation(summary = "根据类型获取配置列表", description = "根据配置类型获取所有配置")
    @GetMapping("/type/{configType}")
    public R getConfigsByType(
        @Parameter(description = "配置类型", required = true) @PathVariable("configType") String configType) {
        try {
            List<Config> configs = configService.getConfigsByType(configType);
            return R.ok().put("data", configs);
        } catch (Exception e) {
            log.error("根据类型获取配置失败，configType: {}", configType, e);
            return R.error("获取配置失败");
        }
    }

    /**
     * 创建配置
     */
    @Operation(summary = "创建配置", description = "新增配置信息")
    @PostMapping
    public R saveConfig(
        @Parameter(description = "配置信息", required = true) @RequestBody Config config) {
        try {
            boolean result = configService.save(config);
            if (result) {
                return R.ok("配置保存成功");
            } else {
                return R.error("保存失败");
            }
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 批量保存配置
     */
    @Operation(summary = "批量保存配置", description = "批量保存多个配置")
    @PostMapping("/batch")
    public R saveConfigs(
        @Parameter(description = "配置列表", required = true) @RequestBody List<Config> configs) {
        try {
            boolean result = configService.saveConfigs(configs);
            if (result) {
                return R.ok("配置批量保存成功");
            } else {
                return R.error("保存失败");
            }
        } catch (Exception e) {
            log.error("批量保存配置失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新配置
     */
    @Operation(summary = "更新配置", description = "根据配置 ID 更新配置信息")
    @PutMapping("/{id}")
    public R updateConfig(
        @Parameter(description = "配置 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "配置信息", required = true) @RequestBody Config config) {
        try {
            config.setId(id);
            boolean result = configService.updateById(config);
            if (result) {
                return R.ok("配置更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改配置失败，ID: {}", config.getId(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新配置
     */
    @PatchMapping("/{id}")
    public R partialUpdateConfig(@PathVariable("id") Long id, @RequestBody Config config) {
        try {
            config.setId(id);
            boolean result = configService.updateById(config);
            if (result) {
                return R.ok("配置更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新配置失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新配置值
     */
    @PatchMapping("/{id}/value")
    public R updateConfigValue(@PathVariable("id") Long configId,
                              @RequestParam String configValue) {
        try {
            Config config = configService.getById(configId);
            if (config == null) {
                return R.error("配置不存在");
            }
            
            config.setConfigValue(configValue);
            boolean result = configService.updateById(config);
            if (result) {
                return R.ok("配置值更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新配置值失败，configId: {}", configId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 通过键名更新配置值
     */
    @PatchMapping("/key/{configKey}/value")
    public R updateConfigValueByKey(@PathVariable("configKey") String configKey,
                                   @RequestParam String configValue) {
        try {
            boolean result = configService.updateConfigValue(configKey, configValue);
            if (result) {
                return R.ok("配置值更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新配置值失败，configKey: {}", configKey, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用配置
     */
    @PatchMapping("/{id}/status")
    public R updateStatus(@PathVariable("id") Long configId,
                         @RequestParam Integer status) {
        try {
            boolean result = configService.updateStatus(configId, status);
            if (result) {
                return R.ok("状态更新成功");
            } else {
                return R.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新配置状态失败，ID: {}", configId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除配置（单个）
     */
    @DeleteMapping("/{id}")
    public R deleteConfig(@PathVariable("id") Long id) {
        try {
            configService.removeById(id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除配置失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除配置
     */
    @PostMapping("/batch-delete")
    public R batchDeleteConfigs(@RequestBody Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的配置");
            }
            
            List<Long> configIds = Arrays.stream(ids).collect(Collectors.toList());
            boolean result = configService.deleteConfigs(configIds);
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return R.error("删除失败");
        }
    }
}
