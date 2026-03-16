package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gcs.entity.Role;
import com.gcs.entity.view.RoleView;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 角色数据访问接口
 * 提供角色相关的数据库操作
 * @author 
 * @email 
 * @date 2026-03-04
 */
public interface RoleDao extends BaseMapper<Role> {
    
    /**
     * 查询角色列表视图
     * @param wrapper 查询条件包装器
     * @return 角色列表
     */
    List<Role> selectListView(@Param("ew") Wrapper<Role> wrapper);

    /**
     * 分页查询角色列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 角色列表
     */
    List<Role> selectListView(IPage<Role> page, @Param("ew") Wrapper<Role> wrapper);
    
    /**
     * 查询角色视图
     * @param wrapper 查询条件包装器
     * @return 角色视图
     */
    RoleView selectView(@Param("ew") Wrapper<Role> wrapper);
    
    /**
     * 根据角色名称查询角色
     * @param roleName 角色名称
     * @return 角色信息
     */
    Role selectByRoleName(@Param("roleName") String roleName);

    /**
     * 根据表名查询角色
     * @param tableName 表名
     * @return 角色信息
     */
}
