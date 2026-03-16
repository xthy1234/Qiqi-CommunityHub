package com.gcs.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

import com.gcs.dao.RoleDao;
import com.gcs.entity.Role;
import com.gcs.entity.view.RoleView;
import com.gcs.service.RoleService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import lombok.extern.slf4j.Slf4j;

/**
 * 角色服务实现类
 * 提供角色相关的业务逻辑处理
 * @author 
 * @date 2026-03-04
 */
@Slf4j
@Service("roleService")
public class RoleServiceImpl extends ServiceImpl<RoleDao, Role> implements RoleService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);
        
        IPage<Role> rolePage = new Query<Role>(params).getPage();
        IPage<Role> resultPage = this.page(rolePage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }

    @Override
    public List<Role> selectListView(Wrapper<Role> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectListView(queryWrapper);
    }

    @Override
    public RoleView selectView(Wrapper<Role> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectView(queryWrapper);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Role> queryWrapper) {
        validateQueryParams(params, queryWrapper);

        IPage<Role> rolePage = new Query<Role>(params).getPage();
        List<Role> roleList = baseMapper.selectListView(rolePage, queryWrapper);
        rolePage.setRecords(roleList);
        
        return new PageUtils(rolePage);
    }
    
    @Override
    public Role getRoleByRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return null;
        }
        
        return this.getOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, roleName));
    }
    


    @Override
    public boolean createRole(Role role) {
        validateRoleForCreate(role);
        
        if (isRoleNameUnique(role.getRoleName(), null)) {
            throw new RuntimeException("角色名称已存在");
        }

        role.setCreateTime(LocalDateTime.now());
        
        return this.save(role);
    }

    @Override
    public boolean updateRole(Role role) {
        validateRoleForUpdate(role);
        
        Role existingRole = getRoleByRoleName(role.getRoleName());
        if (existingRole != null && !existingRole.getId().equals(role.getId())) {
            throw new RuntimeException("角色名称已存在");
        }

        role.setUpdateTime(LocalDateTime.now());
        return this.updateById(role);
    }

    @Override
    public boolean deleteRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色 ID 不能为空");
        }

        Role role = this.getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        return this.removeById(roleId);
    }

    @Override
    public boolean isRoleNameUnique(String roleName, Long excludeRoleId) {
        if (!StringUtils.hasText(roleName)) {
            return true;
        }
        
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getRoleName, roleName);
        if (excludeRoleId != null) {
            queryWrapper.ne(Role::getId, excludeRoleId);
        }
        
        return this.count(queryWrapper) == 0;
    }

    // ==================== 私有验证方法 ====================

    private void validateParams(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(params)) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    private void validateWrapper(Wrapper<Role> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    private void validateQueryParams(Map<String, Object> params, Wrapper<Role> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    private void validateRoleForCreate(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("角色信息不能为空");
        }
        if (!StringUtils.hasText(role.getRoleName())) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
    }

    private void validateRoleForUpdate(Role role) {
        if (role == null || role.getId() == null) {
            throw new IllegalArgumentException("角色信息不完整");
        }
        if (!StringUtils.hasText(role.getRoleName())) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
    }
}
