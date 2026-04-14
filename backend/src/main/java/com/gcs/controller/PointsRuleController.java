package com.gcs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.PointsRule;
import com.gcs.service.PointsRuleService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.MPUtil;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分规则控制器
 */
@Slf4j
@Tag(name = "积分规则管理", description = "积分规则的 CRUD 操作")
@RestController
@RequestMapping("/points-rules")
public class PointsRuleController {

    @Autowired
    private PointsRuleService pointsRuleService;
    
    @Autowired
    private AuthUtils authUtils;
    
    @Autowired
    private SessionUtils sessionUtils;

    /**
     * 分页查询积分规则列表（管理员）
     */
    @Operation(summary = "分页查询积分规则", description = "管理员分页查询所有积分规则")
    @GetMapping
    public R getPage(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }
            
            QueryWrapper<PointsRule> queryWrapper = new QueryWrapper<>();
            PageUtils page = pointsRuleService.queryPage(params,
                    MPUtil.sort(MPUtil.between(queryWrapper, params), params));
            
            log.info("管理员查询积分规则，操作人ID: {}, 结果数: {}", currentUserId, page.getList().size());
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("查询积分规则失败", e);
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有启用的积分规则（用户端）
     */
    @Operation(summary = "获取启用的积分规则", description = "查询所有已启用的积分规则")
    @IgnoreAuth
    @GetMapping("/enabled")
    public R getEnabledRules() {
        try {
            QueryWrapper<PointsRule> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("enabled", true);
            List<PointsRule> rules = pointsRuleService.list(queryWrapper);
            
            return R.ok().put("data", rules);
        } catch (Exception e) {
            log.error("查询启用规则失败", e);
            return R.error("查询失败");
        }
    }

    /**
     * 获取积分规则详情
     */
    @Operation(summary = "获取规则详情", description = "根据 ID 或规则代码获取详情")
    @GetMapping("/{id}")
    public R getRuleInfo(@PathVariable Long id) {
        try {
            PointsRule rule = pointsRuleService.getById(id);
            if (rule == null) {
                return R.error("规则不存在");
            }
            return R.ok().put("data", rule);
        } catch (Exception e) {
            log.error("获取规则详情失败", e);
            return R.error("获取失败");
        }
    }

    /**
     * 根据规则代码获取规则
     */
    @Operation(summary = "根据代码获取规则", description = "通过规则代码查询")
    @IgnoreAuth
    @GetMapping("/code/{ruleCode}")
    public R getRuleByCode(@PathVariable String ruleCode) {
        try {
            PointsRule rule = pointsRuleService.getRule(ruleCode);
            if (rule == null) {
                return R.error("规则不存在");
            }
            return R.ok().put("data", rule);
        } catch (Exception e) {
            log.error("获取规则失败", e);
            return R.error("获取失败");
        }
    }

    /**
     * 创建积分规则（管理员）
     */
    @Operation(summary = "创建积分规则", description = "新增积分规则配置")
    @PostMapping
    public R createRule(@Valid @RequestBody PointsRule rule, HttpServletRequest request) {
        try {
            // 验证登录和管理员权限
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }
            
            // 检查规则代码是否已存在
            QueryWrapper<PointsRule> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("rule_code", rule.getRuleCode());
            long count = pointsRuleService.count(queryWrapper);
            if (count > 0) {
                return R.error("规则代码已存在");
            }
            
            boolean result = pointsRuleService.save(rule);
            if (result) {
                log.info("管理员创建积分规则，规则代码: {}, 操作人ID: {}", rule.getRuleCode(), currentUserId);
                return R.ok("创建成功");
            } else {
                return R.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建规则失败", e);
            return R.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新积分规则（管理员）
     */
    @Operation(summary = "更新积分规则", description = "修改积分规则配置")
    @PutMapping("/{id}")
    public R updateRule(@PathVariable Long id, @Valid @RequestBody PointsRule rule, HttpServletRequest request) {
        try {
            // 验证登录和管理员权限
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }
            
            PointsRule existingRule = pointsRuleService.getById(id);
            if (existingRule == null) {
                return R.error("规则不存在");
            }
            
            rule.setId(id);
            boolean result = pointsRuleService.updateById(rule);
            if (result) {
                log.info("管理员更新积分规则，规则ID: {}, 操作人ID: {}", id, currentUserId);
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新规则失败", e);
            return R.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 启用/禁用积分规则（管理员）
     */
    @Operation(summary = "切换规则状态", description = "启用或禁用指定规则")
    @PatchMapping("/{id}/toggle")
    public R toggleRule(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 验证登录和管理员权限
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }
            
            PointsRule rule = pointsRuleService.getById(id);
            if (rule == null) {
                return R.error("规则不存在");
            }
            
            rule.setEnabled(!rule.getEnabled());
            boolean result = pointsRuleService.updateById(rule);
            
            if (result) {
                String status = rule.getEnabled() ? "启用" : "禁用";
                log.info("管理员{}积分规则，规则ID: {}, 操作人ID: {}", status, id, currentUserId);
                return R.ok("已" + status);
            } else {
                return R.error("操作失败");
            }
        } catch (Exception e) {
            log.error("切换规则状态失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 删除积分规则（管理员）
     */
    @Operation(summary = "删除积分规则", description = "删除指定的积分规则")
    @DeleteMapping("/{id}")
    public R deleteRule(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 验证登录和管理员权限
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }
            
            boolean result = pointsRuleService.removeById(id);
            if (result) {
                log.info("管理员删除积分规则，规则ID: {}, 操作人ID: {}", id, currentUserId);
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除规则失败", e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除积分规则（管理员）
     */
    @Operation(summary = "批量删除规则", description = "批量删除多个积分规则")
    @PostMapping("/batch-delete")
    public R batchDeleteRules(@RequestBody Long[] ids, HttpServletRequest request) {
        try {
            // 验证登录和管理员权限
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }
            
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的规则");
            }
            
            List<Long> idList = Arrays.asList(ids);
            boolean result = pointsRuleService.removeByIds(idList);
            
            if (result) {
                log.info("管理员批量删除积分规则，数量: {}, 操作人ID: {}", ids.length, currentUserId);
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除规则失败", e);
            return R.error("删除失败");
        }
    }
}
