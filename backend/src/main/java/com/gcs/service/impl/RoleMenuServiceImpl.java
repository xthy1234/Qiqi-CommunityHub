package com.gcs.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcs.dao.RoleMenuDao;
import com.gcs.entity.RoleMenu;
import com.gcs.service.RoleMenuService;

import lombok.extern.slf4j.Slf4j;

/**
 * 角色菜单关联服务实现类
 * 提供角色菜单权限相关的业务逻辑处理
 * @author 
 * @date 2026-03-04
 */
@Slf4j
@Service("roleMenuService")
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuDao, RoleMenu> implements RoleMenuService {

    @Override
    public List<RoleMenu> getByRoleId(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByRoleId(roleId);
    }

    @Override
    public List<RoleMenu> getByMenuId(Long menuId) {
        if (menuId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByMenuId(menuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色 ID 不能为空");
        }

        // 先删除原有的权限
        baseMapper.deleteByRoleId(roleId);

        if (menuIds == null || menuIds.isEmpty()) {
            return true;
        }

        // 批量插入新权限
        List<RoleMenu> roleMenus = new ArrayList<>();
        for (Long menuId : menuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenu.setButtons(new ArrayList<>());
            roleMenus.add(roleMenu);
        }

        return baseMapper.batchInsert(roleMenus) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRoleMenus(List<RoleMenu> roleMenus) {
        if (roleMenus == null || roleMenus.isEmpty()) {
            return true;
        }

        // 验证数据
        for (RoleMenu roleMenu : roleMenus) {
            if (roleMenu.getRoleId() == null || roleMenu.getMenuId() == null) {
                throw new IllegalArgumentException("角色 ID 和菜单 ID 不能为空");
            }
            if (roleMenu.getButtons() == null) {
                roleMenu.setButtons(new ArrayList<>());
            }
        }

        // 先删除第一个角色的所有权限（假设都是同一个角色）
        Long roleId = roleMenus.get(0).getRoleId();
        baseMapper.deleteByRoleId(roleId);

        return baseMapper.batchInsert(roleMenus) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRoleId(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色 ID 不能为空");
        }
        return baseMapper.deleteByRoleId(roleId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleMenu(Long roleId, Long menuId) {
        if (roleId == null || menuId == null) {
            throw new IllegalArgumentException("角色 ID 和菜单 ID 不能为空");
        }

        QueryWrapper<RoleMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId).eq("menu_id", menuId);
        
        return this.remove(queryWrapper);
    }
}
