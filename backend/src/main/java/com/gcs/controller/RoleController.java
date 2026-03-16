package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.Role;
import com.gcs.entity.view.RoleView;
import com.gcs.service.RoleService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 角色控制器
 * 提供角色相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-03-04
 */
@Slf4j
@Tag(name = "角色管理", description = "角色相关的 RESTful API 接口")
@RestController
@RequestMapping("/roles")
public class RoleController {
    
    @Autowired
    private RoleService roleService;

    /**
     * 获取角色分页列表（后台）
     */
    @Operation(summary = "获取角色分页列表", description = "分页查询角色列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "角色查询条件") Role role) {
        try {
            QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
            PageUtils page = roleService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, role), params), params));
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取角色分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取角色列表（前端，无分页）
     */
    @Operation(summary = "获取所有角色列表", description = "获取所有角色数据，不分页")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @IgnoreAuth
    @GetMapping("/all")
    public R getRoleList(
        @Parameter(description = "角色查询条件") @RequestParam Map<String, Object> params, 
        @Parameter(description = "角色查询条件") Role role) {
        try {
            QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
            queryWrapper.allEq(MPUtil.allEQMapPre(role, "role"));
            return R.ok().put("data", roleService.selectListView(queryWrapper));
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 获取角色详情
     */
    @Operation(summary = "获取角色详情", description = "根据角色 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "角色不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{id}")
    public R getRole(
        @Parameter(description = "角色 ID", required = true) @PathVariable("id") Long id) {
        try {
            Role role = roleService.getById(id);
            if (role == null) {
                return R.error("角色不存在");
            }
            return R.ok().put("data", role);
        } catch (Exception e) {
            log.error("获取角色详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 统计角色数量
     */
    @Operation(summary = "统计角色数量", description = "根据角色名称统计角色总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/count")
    public R countRoles(
        @Parameter(description = "角色名称 (可选)") @RequestParam(required = false) String roleName) {
        try {
            QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
            if (roleName != null && !roleName.isEmpty()) {
                queryWrapper.like("role_name", roleName);
            }
            Long count = roleService.count(queryWrapper);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计角色数量失败", e);
            return R.error("统计失败");
        }
    }

    /**
     * 创建角色
     */
    @Operation(summary = "创建角色", description = "新增角色信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @PostMapping
    public R createRole(
        @Parameter(description = "角色信息", required = true) @Valid @RequestBody Role role) {
        try {
            boolean result = roleService.createRole(role);
            if (result) {
                return R.ok("角色创建成功");
            } else {
                return R.error("创建失败");
            }
        } catch (Exception e) {
            log.error("保存角色失败，名称：{}", role.getRoleName(), e);
            return R.error(e.getMessage());
        }
    }
    
    /**
     * 更新角色信息
     */
    @Operation(summary = "更新角色信息", description = "根据角色 ID 更新角色信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    @Transactional
    public R updateRole(
        @Parameter(description = "角色 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "角色信息", required = true) @Valid @RequestBody Role role) {
        try {
            role.setId(id);
            boolean result = roleService.updateRole(role);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改角色失败，ID: {}", role.getId(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新角色
     */
    @Operation(summary = "部分更新角色", description = "根据角色 ID 部分更新角色信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateRole(
        @Parameter(description = "角色 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "角色信息") @RequestBody Role role) {
        try {
            role.setId(id);
            boolean result = roleService.updateRole(role);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新角色失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除角色（单个）
     */
    @Operation(summary = "删除角色", description = "根据角色 ID 删除单个角色")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteRole(
        @Parameter(description = "角色 ID", required = true) @PathVariable("id") Long id) {
        try {
            boolean result = roleService.removeById(id);
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除角色失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除角色
     */
    @Operation(summary = "批量删除角色", description = "批量删除多个角色")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的角色"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping
    public R deleteRoles(
        @Parameter(description = "角色 ID 数组", required = true) @RequestBody Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的角色");
            }
            
            boolean result = roleService.removeByIds(Arrays.asList(ids));
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除角色失败", e);
            return R.error("删除失败");
        }
    }
}
