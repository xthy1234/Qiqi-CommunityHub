package com.gcs.service;
import com.baomidou.mybatisplus.extension.service.IService; import com.gcs.entity.RoleMenu;
import java.util.List;
/**
 角色菜单关联服务接口
 提供角色菜单权限相关的业务操作
 @author
 @date 2026-03-04 */ public interface RoleMenuService extends IService<RoleMenu> {
    /**
     根据角色 ID 查询菜单权限

     @param roleId 角色 ID
     @return 角色菜单权限列表 */ List<RoleMenu> getByRoleId(Long roleId);
    /**
     根据菜单 ID 查询角色权限

     @param menuId 菜单 ID
     @return 角色菜单权限列表 */ List<RoleMenu> getByMenuId(Long menuId);
    /**
     保存角色菜单权限

     @param roleId 角色 ID
     @param menuIds 菜单 ID 列表
     @return 保存结果 */ boolean saveRoleMenus(Long roleId, List<Long> menuIds);
    /**
     保存角色菜单权限（包含按钮权限）

     @param roleMenus 角色菜单权限列表
     @return 保存结果 */ boolean saveRoleMenus(List<RoleMenu> roleMenus);
    /**
     删除角色菜单权限

     @param roleId 角色 ID
     @return 删除结果 */ boolean deleteByRoleId(Long roleId);
    /**
     删除角色菜单权限

     @param roleId 角色 ID
     @param menuId 菜单 ID
     @return 删除结果 */ boolean deleteRoleMenu(Long roleId, Long menuId); }