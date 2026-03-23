package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.CommonStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户实体类
 * 数据库通用操作实体类（普通增删改查）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户实体")
@TableName("users")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号
     */
    @Schema(description = "用户账号", example = "user@example.com")
    private String account;

    /**
     * 用户密码
     */
    @Schema(description = "用户密码", example = "password123")
    private String password;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    /**
     * 头像 URL
     */
    @Schema(description = "头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;

    /**
     * 性别 (0:保密，1:男，2:女)
     */
    @Schema(description = "性别 (0:保密，1:男，2:女)", example = "1")
    private Integer gender;

    /**
     * 电话号码
     */
    @Schema(description = "电话号码", example = "13800138000")
    private String phone;

    /**
     * 邮箱地址
     */
    @Schema(description = "邮箱地址", example = "user@example.com")
    private String email;

    @Schema(description = "角色 ID", example = "1")
    private Long roleId;

    /**
     * 生日
     */
    @Schema(description = "生日", example = "2000-01-01")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 个人签名
     */
    @Schema(description = "个人签名", example = "生活不止眼前的苟且，还有诗和远方")
    private String signature;

    /**
     * 状态 (0:启用，1:禁用)
     */
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private CommonStatus status = CommonStatus.ENABLED;

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
     * 最后登录时间
     */
    @Schema(description = "最后登录时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;


    /**
     * 最后在线时间
     */
    @Schema(description = "最后在线时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOnlineTime;

    /**
     * 最后登录 IP
     */
    @Schema(description = "最后登录 IP", example = "192.168.1.1")
    private String lastLoginIp;

    // 泛型构造函数（保持向后兼容）
    public <T> User(T t) {
        // 简单的属性复制，避免使用过时的BeanUtils
        if (t != null) {
            // 这里可以根据实际需求实现属性复制逻辑
            // 或者使用更现代的映射工具如MapStruct
        }
    }
}


