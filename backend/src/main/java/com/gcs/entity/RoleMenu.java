package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色菜单关联实体类
 * 角色与菜单权限的关联关系
 * @author 
 * @email 
 * @date 2026-03-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色菜单权限关联实体")
@TableName("role_menu")
public class RoleMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID
     */
    @Schema(description = "角色 ID", example = "1")
    private Long roleId;

    /**
     * 菜单 ID
     */
    @Schema(description = "菜单 ID", example = "1")
    private Long menuId;

    /**
     * 按钮权限列表（如 ['新增','查看','修改']）
     */
    @Schema(description = "按钮权限列表", example = "[\"新增\",\"查看\",\"修改\"]")
    @TableField(typeHandler = org.apache.ibatis.type.ArrayTypeHandler.class)
    private List<String> buttons;
}
