package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色实体类
 * 系统角色权限管理
 * @author 
 * @email 
 * @date 2026-03-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色实体")
@TableName("role")
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称
     */
    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    /**
     * 是否有后台登录权限
     */
    @Schema(description = "是否有后台登录权限", example = "true")
    private Boolean hasBackLogin;

    /**
     * 是否有后台注册权限
     */
    @Schema(description = "是否有后台注册权限", example = "false")
    private Boolean hasBackRegister;

    /**
     * 是否有前台登录权限
     */
    @Schema(description = "是否有前台登录权限", example = "true")
    private Boolean hasFrontLogin;

    /**
     * 是否有前台注册权限
     */
    @Schema(description = "是否有前台注册权限", example = "true")
    private Boolean hasFrontRegister;
    
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
}
