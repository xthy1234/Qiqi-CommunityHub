package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单基础视图对象 - 用于列表展示
 */
@Data
@Schema(description = "菜单基础视图对象")
public class MenuVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "菜单名称", example = "用户管理")
    private String name;
    
    @Schema(description = "菜单类型（0:目录，1:菜单，2:按钮）", example = "1")
    private Integer type;
    
    @Schema(description = "父级菜单 ID", example = "0")
    private Long parentId;
    
    @Schema(description = "菜单图标", example = "user")
    private String icon;
    
    @Schema(description = "菜单路径", example = "/system/user")
    private String path;
    
    @Schema(description = "组件路径", example = "system/user/index")
    private String component;
    
    @Schema(description = "排序字段", example = "1")
    private Integer sort;
    
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private Integer status;
    
    @Schema(description = "备注信息", example = "系统管理菜单")
    private String remark;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
