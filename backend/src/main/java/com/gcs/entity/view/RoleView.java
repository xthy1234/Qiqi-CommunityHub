package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 角色视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-03-04
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("role")
public class RoleView extends Role implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：状态描述
     */
    private String statusDescription;

    /**
     * 扩展字段：权限描述
     */
    private String permissionDescription;

    /**
     * 构造函数
     * @param role 原始角色对象
     */
    public RoleView(Role role) {
        if (role != null) {
            this.setId(role.getId());
            this.setRoleName(role.getRoleName());
            this.setHasBackLogin(role.getHasBackLogin());
            this.setHasBackRegister(role.getHasBackRegister());
            this.setHasFrontLogin(role.getHasFrontLogin());
            this.setHasFrontRegister(role.getHasFrontRegister());
            this.setCreateTime(role.getCreateTime());
            this.setUpdateTime(role.getUpdateTime());
        }
    }
}
