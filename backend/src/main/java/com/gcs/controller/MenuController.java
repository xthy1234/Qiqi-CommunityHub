package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.Menu;
import com.gcs.entity.view.MenuView;
import com.gcs.service.MenuService;
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
 * 菜单控制器
 * 提供菜单相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Slf4j
@Tag(name = "菜单管理", description = "菜单相关的 RESTful API 接口")
@RestController
@RequestMapping("/menus")
public class MenuController {
    
    @Autowired
    private MenuService menuService;

    /**
     * 获取菜单分页列表
     */
    @Operation(summary = "获取菜单分页列表", description = "分页查询菜单列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "菜单查询条件") Menu menu) {
        try {
            QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
            PageUtils page = menuService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, menu), params), params));
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取菜单分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取所有菜单列表（无分页）
     */
    @Operation(summary = "获取所有菜单列表", description = "获取所有菜单数据，不分页")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @IgnoreAuth
    @GetMapping("/all")
    public R getAllMenus(
        @Parameter(description = "菜单查询条件") Menu menu) {
        try {
            QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
            // 直接使用不带前缀的字段映射
            queryWrapper.allEq(MPUtil.allEQMap(menu), false);
            return R.ok().put("data", menuService.selectListView(queryWrapper));
        } catch (Exception e) {
            log.error("获取所有菜单失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 获取菜单详情
     */
    @Operation(summary = "获取菜单详情", description = "根据菜单 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "菜单不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @IgnoreAuth
    @GetMapping("/{id}")
    public R getMenu(
        @Parameter(description = "菜单 ID", required = true) @PathVariable("id") Long id) {
        try {
            Menu menu = menuService.getById(id);
            if (menu == null) {
                return R.error("菜单不存在");
            }
            return R.ok().put("data", menu);
        } catch (Exception e) {
            log.error("获取菜单详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 获取菜单树结构
     */
    @Operation(summary = "获取菜单树结构", description = "获取菜单的树形层级结构")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @IgnoreAuth
    @GetMapping("/tree")
    public R getMenuTree() {
        try {
            List<Menu> menuTree = menuService.getTreeList();
            return R.ok().put("data", menuTree);
        } catch (Exception e) {
            log.error("获取菜单树失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 获取当前登录用户的菜单权限
     */
    @Operation(summary = "获取用户菜单权限", description = "根据当前登录用户的角色 ID 查询对应的菜单权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/auth")
    @IgnoreAuth
    public R getUserMenus(
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            // 从 session 中获取用户角色信息
            Long userId;
            Long roleId;
            try {
                roleId = (Long) request.getSession().getAttribute("roleId");
                userId = (Long) request.getSession().getAttribute("userId");
            } catch (NumberFormatException e) {
                log.warn("角色 ID 转换失败");
                return R.error("角色信息异常");
            }


            if (userId == null||roleId==null) {
                roleId = 3L;//游客访问
            }

            List<Menu> userMenus;

            // 1是普通用户
            //2是管理员
            //3是游客
            //调试用，现在还没设置游客能访问的菜单
            if(roleId==3L){
                roleId=1L;
            }

            if (roleId <= 0) {
                return R.error("角色信息异常");
            }
            userMenus = menuService.getMenusByRoleId(roleId);
            return R.ok().put("data", userMenus);
        } catch (Exception e) {
            log.error("获取用户菜单失败", e);
            return R.error("获取菜单失败");
        }
    }

    /**
     * 获取子菜单
     */
    @Operation(summary = "获取子菜单", description = "根据父级菜单 ID 获取其所有子菜单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{parentId}/children")
    public R getChildrenByParentId(
        @Parameter(description = "父级菜单 ID", required = true) @PathVariable("parentId") Long parentId) {
        try {
            List<Menu> children = menuService.getByParentId(parentId);
            return R.ok().put("data", children);
        } catch (Exception e) {
            log.error("获取子菜单失败，parentId: {}", parentId, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 统计菜单数量
     */
    @Operation(summary = "统计菜单数量", description = "根据菜单类型和状态统计菜单总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/count")
    public R countMenus(
        @Parameter(description = "菜单类型 (可选)") @RequestParam(required = false) Integer menuType,
        @Parameter(description = "状态 (可选)") @RequestParam(required = false) Integer status) {
        try {
            QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
            if (menuType != null) {
                queryWrapper.eq("type", menuType);
            }
            if (status != null) {
                queryWrapper.eq("status", status);
            }
            
            Long count = menuService.count(queryWrapper);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计菜单数量失败", e);
            return R.error("统计失败");
        }
    }

    /**
     * 创建菜单
     */
    @Operation(summary = "创建菜单", description = "新增菜单信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @PostMapping
    public R createMenu(
        @Parameter(description = "菜单信息", required = true) @Valid @RequestBody Menu menu) {
        try {
            boolean result = menuService.createMenu(menu);
            if (result) {
                return R.ok("菜单创建成功");
            } else {
                return R.error("创建失败");
            }
        } catch (Exception e) {
            log.error("保存菜单失败，名称：{}", menu.getName(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新菜单信息
     */
    @Operation(summary = "更新菜单信息", description = "根据菜单 ID 更新菜单信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    @Transactional
    public R updateMenu(
        @Parameter(description = "菜单 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "菜单信息", required = true) @Valid @RequestBody Menu menu) {
        try {
            menu.setId(id);
            boolean result = menuService.updateMenu(menu);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改菜单失败，ID: {}", menu.getId(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新菜单
     */
    @Operation(summary = "部分更新菜单", description = "根据菜单 ID 部分更新菜单信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateMenu(
        @Parameter(description = "菜单 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "菜单信息") @RequestBody Menu menu) {
        try {
            menu.setId(id);
            boolean result = menuService.updateMenu(menu);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新菜单失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新菜单状态
     */
    @Operation(summary = "更新菜单状态", description = "根据菜单 ID 更新菜单的启用/禁用状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "状态更新成功"),
        @ApiResponse(responseCode = "400", description = "状态不能为空或菜单不存在"),
        @ApiResponse(responseCode = "500", description = "状态更新失败")
    })
    @PatchMapping("/{id}/status")
    public R updateStatus(
        @Parameter(description = "菜单 ID", required = true) @PathVariable("id") Long menuId,
        @Parameter(description = "状态参数", required = true) @RequestBody Map<String, Object> params) {
        try {
            Menu menu = menuService.getById(menuId);
            if (menu == null) {
                return R.error("菜单不存在");
            }
            
            Integer status = (Integer) params.get("status");
            if (status == null) {
                return R.error("状态不能为空");
            }
            
            menu.setStatus(status);
            boolean result = menuService.updateMenu(menu);
            if (result) {
                return R.ok("状态更新成功");
            } else {
                return R.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新菜单状态失败，ID: {}", menuId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除菜单（单个）
     */
    @Operation(summary = "删除菜单", description = "根据菜单 ID 删除单个菜单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteMenu(
        @Parameter(description = "菜单 ID", required = true) @PathVariable("id") Long id) {
        try {
            boolean result = menuService.removeById(id);
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除菜单失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除菜单
     */
    @Operation(summary = "批量删除菜单", description = "批量删除多个菜单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的菜单"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping
    public R deleteMenus(
        @Parameter(description = "菜单 ID 数组", required = true) @RequestBody Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的菜单");
            }
            
            boolean result = menuService.removeByIds(Arrays.asList(ids));
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除菜单失败", e);
            return R.error("删除失败");
        }
    }
}
