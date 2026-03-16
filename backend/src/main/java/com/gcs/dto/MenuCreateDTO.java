package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单创建请求对象
 */
@Data
@Schema(description = "菜单创建请求对象")
public class MenuCreateDTO {
    
    @NotBlank(message = "菜单名称不能为空")
    @Schema(description = "菜单名称", required = true, example = "用户管理")
    private String name;
    
    @NotNull(message = "菜单类型不能为空")
    @Schema(description = "菜单类型（0:目录，1:菜单，2:按钮）", required = true, example = "1")
    private Integer type;
    
    @Schema(description = "父级菜单 ID", example = "0")
    private Long parentId;
    
    @Schema(description = "菜单图标", example = "user")
    private String icon;
    
    @Schema(description = "菜单路径", example = "/system/user")
    private String path;
    
    @Schema(description = "组件路径", example = "system/user/index")
    private String component;
    
    @NotNull(message = "排序字段不能为空")
    @Schema(description = "排序字段", required = true, example = "1")
    private Integer sort;
    
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private Integer status;
    
    @Schema(description = "备注信息", example = "系统管理菜单")
    private String remark;
}
