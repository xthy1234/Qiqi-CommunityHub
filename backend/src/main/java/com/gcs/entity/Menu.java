package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜单实体类
 * 数据库通用操作实体类（普通增删改查）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜单实体")
@TableName("menu")
public class Menu implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称", example = "用户管理")
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /**
     * 菜单类型（目录/菜单/按钮）
     */
    @Schema(description = "菜单类型 (0:目录，1:菜单，2:按钮)", example = "1")
    private Integer type = 1;

    /**
     * 父级菜单 ID
     */
    @Schema(description = "父级菜单 ID", example = "0")
    private Long parentId;

    /**
     * 菜单图标
     */
    @Schema(description = "菜单图标", example = "user")
    private String icon;

    /**
     * 菜单路径
     */
    @Schema(description = "菜单路径", example = "/system/user")
    private String path;

    @Schema(description = "组件路径", example = "system/user/index")
    private String component;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "1")
    private Integer sort = 0;

    /**
     * 状态 (0:启用，1:禁用)
     */
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private Integer status = 0;
    
    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean isDeleted = false;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息", example = "系统管理菜单")
    private String remark;

}
