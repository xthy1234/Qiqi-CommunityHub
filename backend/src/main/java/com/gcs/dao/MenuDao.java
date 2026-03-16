package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gcs.entity.Menu;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 菜单数据访问接口
 * 提供菜单相关的数据库操作
 * @author
 * @email
 * @date 2026-03-04
 */
public interface MenuDao extends BaseMapper<Menu> {

    /**
     * 查询菜单列表视图
     * @param wrapper 查询条件包装器
     * @return 菜单列表
     */
    List<Menu> selectListView(@Param("ew") Wrapper<Menu> wrapper);

    /**
     * 分页查询菜单列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 菜单列表
     */
    List<Menu> selectListView(IPage<Menu> page, @Param("ew") Wrapper<Menu> wrapper);

    /**
     * 根据父菜单 ID 查询子菜单
     * @param parentId 父菜单 ID
     * @return 子菜单列表
     */
    List<Menu> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询菜单树形结构
     * @return 菜单树形列表
     */
    List<Menu> selectTreeList();

    /**
     * 根据角色 ID 查询菜单 ID 列表
     * @param roleIds 角色 ID 列表
     * @return 菜单 ID 列表
     */
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}

