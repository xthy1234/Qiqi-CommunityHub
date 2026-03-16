package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.RoleMenu;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 角色菜单关联数据访问接口
 * 提供角色菜单权限相关的数据库操作
 * @author 
 * @email 
 * @date 2026-03-04
 */
public interface RoleMenuDao extends BaseMapper<RoleMenu> {
    
    /**
     * 根据角色 ID 查询菜单权限
     * @param roleId 角色 ID
     * @return 角色菜单权限列表
     */
    List<RoleMenu> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据菜单 ID 查询角色权限
     * @param menuId 菜单 ID
     * @return 角色菜单权限列表
     */
    List<RoleMenu> selectByMenuId(@Param("menuId") Long menuId);

    /**
     * 删除角色的菜单权限
     * @param roleId 角色 ID
     * @return 删除结果
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色的指定菜单权限
     * @param roleId 角色 ID
     * @param menuId 菜单 ID
     * @return 删除结果
     */
    int deleteByRoleIdAndMenuId(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    /**
     * 批量插入角色菜单权限
     * @param roleMenus 角色菜单权限列表
     * @return 插入结果
     */
    int batchInsert(@Param("list") List<RoleMenu> roleMenus);

    /**
     * 根据角色 ID 查询菜单 ID 列表
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
