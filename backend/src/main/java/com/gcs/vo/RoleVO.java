package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色基础视图对象
 */
@Data
@Schema(description = "角色基础视图对象")
public class RoleVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "角色名称", example = "管理员")
    private String roleName;
    
    @Schema(description = "是否有后台登录权限", example = "true")
    private Boolean hasBackLogin;
    
    @Schema(description = "是否有后台注册权限", example = "false")
    private Boolean hasBackRegister;
    
    @Schema(description = "是否有前台登录权限", example = "true")
    private Boolean hasFrontLogin;
    
    @Schema(description = "是否有前台注册权限", example = "true")
    private Boolean hasFrontRegister;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
