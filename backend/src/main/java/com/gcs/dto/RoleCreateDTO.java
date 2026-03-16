package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 角色创建请求对象
 */
@Data
@Schema(description = "角色创建请求对象")
public class RoleCreateDTO {
    
    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称", required = true, example = "管理员")
    private String roleName;
    
    @Schema(description = "是否有后台登录权限", example = "true")
    private Boolean hasBackLogin;
    
    @Schema(description = "是否有后台注册权限", example = "false")
    private Boolean hasBackRegister;
    
    @Schema(description = "是否有前台登录权限", example = "true")
    private Boolean hasFrontLogin;
    
    @Schema(description = "是否有前台注册权限", example = "true")
    private Boolean hasFrontRegister;
}
