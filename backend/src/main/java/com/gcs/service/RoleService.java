package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Role;
import com.gcs.entity.view.RoleView;
import com.gcs.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 角色服务接口
 * 提供角色相关的业务操作
 * @author 
 * @date 2026-03-04
 */
public interface RoleService extends IService<Role> {

    /**
     * 分页查询角色列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询角色列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 角色列表
     */
    List<Role> selectListView(Wrapper<Role> queryWrapper);

    /**
     * 查询角色视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 角色视图
     */
    RoleView selectView(Wrapper<Role> queryWrapper);

    /**
     * 带条件的分页查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Role> queryWrapper);
    
    /**
     * 根据角色名称查询角色
     *
     * @param roleName 角色名称
     * @return 角色信息
     */
    Role getRoleByRoleName(String roleName);


    /**
     * 创建角色
     *
     * @param role 角色信息
     * @return 创建结果
     */
    boolean createRole(Role role);

    /**
     * 更新角色信息
     *
     * @param role 角色信息
     * @return 更新结果
     */
    boolean updateRole(Role role);

    /**
     * 删除角色
     *
     * @param roleId 角色 ID
     * @return 删除结果
     */
    boolean deleteRole(Long roleId);

    /**
     * 检查角色名称唯一性
     *
     * @param roleName 角色名称
     * @param excludeRoleId 排除的角色 ID
     * @return 是否唯一
     */
    boolean isRoleNameUnique(String roleName, Long excludeRoleId);
}
