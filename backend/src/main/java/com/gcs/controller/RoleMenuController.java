package com.gcs.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.RoleMenu;
import com.gcs.service.RoleMenuService;
import com.gcs.utils.R;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 角色菜单权限控制器
 * 提供角色菜单权限相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-03-04
 */
@Slf4j
@Tag(name = "角色菜单权限管理", description = "角色菜单权限相关的 RESTful API 接口")
@RestController
@RequestMapping("/role-menus")
public class RoleMenuController {
    
    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * 获取角色的菜单权限列表
     */
    @Operation(summary = "获取角色的菜单权限列表", description = "根据角色 ID 获取该角色的所有菜单权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping("/by-role/{roleId}")
    public R getByRoleId(
        @Parameter(description = "角色 ID", required = true) @PathVariable("roleId") Long roleId) {
        try {
            List<RoleMenu> roleMenus = roleMenuService.getByRoleId(roleId);
            return R.ok().put("data", roleMenus);
        } catch (Exception e) {
            log.error("查询角色菜单权限失败，roleId: {}", roleId, e);
            return R.error("查询失败");
        }
    }

    /**
     * 获取菜单的角色权限列表
     */
    @Operation(summary = "获取菜单的角色权限列表", description = "根据菜单 ID 拥有该菜单的所有角色权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping("/by-menu/{menuId}")
    public R getByMenuId(
        @Parameter(description = "菜单 ID", required = true) @PathVariable("menuId") Long menuId) {
        try {
            List<RoleMenu> roleMenus = roleMenuService.getByMenuId(menuId);
            return R.ok().put("data", roleMenus);
        } catch (Exception e) {
            log.error("查询菜单角色权限失败，menuId: {}", menuId, e);
            return R.error("查询失败");
        }
    }

    /**
     * 保存角色的菜单权限
     */
    @Operation(summary = "保存角色的菜单权限", description = "为指定角色保存菜单权限配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "权限保存成功"),
        @ApiResponse(responseCode = "400", description = "保存失败")
    })
    @PostMapping("/by-role/{roleId}")
    @Transactional
    public R saveRoleMenus(
        @Parameter(description = "角色 ID", required = true) @PathVariable("roleId") Long roleId, 
        @Parameter(description = "菜单 ID 列表", required = true) @RequestBody List<Long> menuIds) {
        try {
            boolean result = roleMenuService.saveRoleMenus(roleId, menuIds);
            if (result) {
                return R.ok("权限保存成功");
            } else {
                return R.error("保存失败");
            }
        } catch (Exception e) {
            log.error("保存角色菜单权限失败，roleId: {}", roleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 批量保存角色菜单权限（包含按钮权限）
     */
    @Operation(summary = "批量保存角色菜单权限", description = "批量保存角色与菜单的关联关系，包含按钮权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "权限保存成功"),
        @ApiResponse(responseCode = "400", description = "保存失败")
    })
    @PostMapping
    @Transactional
    public R saveRoleMenusWithButtons(
        @Parameter(description = "角色菜单权限列表", required = true) @Valid @RequestBody List<RoleMenu> roleMenus) {
        try {
            boolean result = roleMenuService.saveRoleMenus(roleMenus);
            if (result) {
                return R.ok("权限保存成功");
            } else {
                return R.error("保存失败");
            }
        } catch (Exception e) {
            log.error("保存角色菜单权限失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除角色的所有菜单权限
     */
    @Operation(summary = "删除角色的所有菜单权限", description = "删除指定角色的所有菜单权限配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "权限删除成功"),
        @ApiResponse(responseCode = "400", description = "删除失败")
    })
    @DeleteMapping("/by-role/{roleId}")
    @Transactional
    public R deleteByRoleId(
        @Parameter(description = "角色 ID", required = true) @PathVariable("roleId") Long roleId) {
        try {
            boolean result = roleMenuService.deleteByRoleId(roleId);
            if (result) {
                return R.ok("权限删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除角色菜单权限失败，roleId: {}", roleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除角色的单个菜单权限
     */
    @Operation(summary = "删除角色的单个菜单权限", description = "删除指定角色的某个菜单权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "权限删除成功"),
        @ApiResponse(responseCode = "400", description = "删除失败")
    })
    @DeleteMapping("/by-role/{roleId}/menu/{menuId}")
    @Transactional
    public R deleteRoleMenu(
        @Parameter(description = "角色 ID", required = true) @PathVariable("roleId") Long roleId, 
        @Parameter(description = "菜单 ID", required = true) @PathVariable("menuId") Long menuId) {
        try {
            boolean result = roleMenuService.deleteRoleMenu(roleId, menuId);
            if (result) {
                return R.ok("权限删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除角色菜单权限失败，roleId: {}, menuId: {}", roleId, menuId, e);
            return R.error(e.getMessage());
        }
    }
}
