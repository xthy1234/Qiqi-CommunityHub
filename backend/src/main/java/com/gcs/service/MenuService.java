package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Menu;
import com.gcs.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 菜单服务接口
 * 提供菜单相关的业务操作
 * @author
 * @date 2026-03-04
 */
public interface MenuService extends IService<Menu> {

    /**
     * 分页查询菜单列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询菜单列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 菜单列表
     */
    List<Menu> selectListView(Wrapper<Menu> queryWrapper);

    /**
     * 带条件的分页查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Menu> queryWrapper);

    /**
     * 查询菜单树形结构
     *
     * @return 菜单树形列表
     */
    List<Menu> getTreeList();

    /**
     * 根据父菜单 ID 查询子菜单
     *
     * @param parentId 父菜单 ID
     * @return 子菜单列表
     */
    List<Menu> getByParentId(Long parentId);

    /**
     * 创建菜单
     *
     * @param menu 菜单信息
     * @return 创建结果
     */
    boolean createMenu(Menu menu);

    /**
     * 更新菜单信息
     *
     * @param menu 菜单信息
     * @return 更新结果
     */
    boolean updateMenu(Menu menu);

    /**
     * 删除菜单
     *
     * @param menuId 菜单 ID
     * @return 删除结果
     */
    boolean deleteMenu(Long menuId);

    /**
     * 根据角色 ID 查询用户有权限的菜单
     *
     * @param roleId 角色 ID
     * @return 用户有权限的菜单列表
     */
    List<Menu> getMenusByRoleId(Long roleId);
}
