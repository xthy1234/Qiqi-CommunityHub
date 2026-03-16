package com.gcs.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.service.RoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

import com.gcs.dao.MenuDao;
import com.gcs.entity.Menu;
import com.gcs.service.MenuService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import lombok.extern.slf4j.Slf4j;

/**
 * 菜单服务实现类
 * 提供菜单相关的业务逻辑处理
 * @author
 * @date 2026-03-04
 */
@Slf4j
@Service("menuService")
public class MenuServiceImpl extends ServiceImpl<MenuDao, Menu> implements MenuService {

    @Autowired
    private RoleMenuService roleMenuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);

        IPage<Menu> menuPage = new Query<Menu>(params).getPage();
        IPage<Menu> resultPage = this.page(menuPage, new QueryWrapper<>());

        return new PageUtils(resultPage);
    }

    @Override
    public List<Menu> selectListView(Wrapper<Menu> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectListView(queryWrapper);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Menu> queryWrapper) {
        validateQueryParams(params, queryWrapper);

        IPage<Menu> menuPage = new Query<Menu>(params).getPage();
        List<Menu> menuList = baseMapper.selectListView(menuPage, queryWrapper);
        menuPage.setRecords(menuList);

        return new PageUtils(menuPage);
    }

    @Override
    public List<Menu> getTreeList() {
        return baseMapper.selectTreeList();
    }

    @Override
    public List<Menu> getByParentId(Long parentId) {
        if (parentId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByParentId(parentId);
    }

    @Override
    public boolean createMenu(Menu menu) {
        validateMenuForCreate(menu);

        menu.setCreateTime(LocalDateTime.now());

        return this.save(menu);
    }

    @Override
    public boolean updateMenu(Menu menu) {
        validateMenuForUpdate(menu);

        menu.setUpdateTime(LocalDateTime.now());
        return this.updateById(menu);
    }

    @Override
    public boolean deleteMenu(Long menuId) {
        if (menuId == null) {
            throw new IllegalArgumentException("菜单 ID 不能为空");
        }

        Menu menu = this.getById(menuId);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }

        return this.removeById(menuId);
    }

    @Override
    public List<Menu> getMenusByRoleId(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        
        // 步骤 1：从 role_menu 表中查询该角色拥有的所有菜单 ID
        List<Long> menuIds = baseMapper.selectMenuIdsByRoleIds(java.util.Arrays.asList(roleId));
        
        if (menuIds == null || menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 步骤 2：根据菜单 ID 列表查询菜单详情
        QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", menuIds)
                   .eq("status", 1) // 只查询启用的菜单
                   .orderByAsc("sort")
                   .orderByDesc("create_time");
        
        return this.list(queryWrapper);
    }

    // ==================== 私有验证方法 ====================

    private void validateParams(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(params)) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    private void validateWrapper(Wrapper<Menu> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    private void validateQueryParams(Map<String, Object> params, Wrapper<Menu> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    private void validateMenuForCreate(Menu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("菜单信息不能为空");
        }
        if (!StringUtils.hasText(menu.getName())) {
            throw new IllegalArgumentException("菜单名称不能为空");
        }

        if (menu.getType() == null) {
            throw new IllegalArgumentException("菜单类型不能为空");
        }
    }

    private void validateMenuForUpdate(Menu menu) {
        if (menu == null || menu.getId() == null) {
            throw new IllegalArgumentException("菜单信息不完整");
        }
        if (!StringUtils.hasText(menu.getName())) {
            throw new IllegalArgumentException("菜单名称不能为空");
        }

    }
}
