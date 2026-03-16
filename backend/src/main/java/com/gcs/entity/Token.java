package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.CommonStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.ibatis.type.JdbcType;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Token 实体类
 * 用于用户身份认证和会话管理
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token 实体（用户认证令牌）")
@TableName("token")
public class Token implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /**
     * 用户账号（用于登录标识）
     */
    @Schema(description = "用户账号", example = "user@example.com")
    private String account;

    /**
     * 用户角色
     */
    @Schema(description = "用户角色 ID", example = "1")
    private Long roleId;

    /**
     * 访问令牌
     */
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    /**
     * 过期时间
     */
    @Schema(description = "过期时间", example = "2026-01-02 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

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
     * 状态 (0:有效，1:无效)
     */
    @Schema(description = "状态 (0:有效，1:无效)", example = "0")
    private CommonStatus status = CommonStatus.ENABLED;

    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean isDeleted = false;

    /**
     * IP 地址
     */
    @Schema(description = "IP 地址", example = "192.168.1.1")
    @TableField(value = "ip_address", jdbcType = JdbcType.VARCHAR)
    private String ipAddress;

    /**
     * 设备信息
     */
    @Schema(description = "设备信息", example = "Chrome/Windows 10")
    private String deviceInfo;


    // 构造函数（保持向后兼容）
    public Token(Long userid, String account, Long roleId, String token, Date expiratedtime) {
        this.userId = userid;
        this.account = account;
        this.roleId = roleId;
        this.accessToken = token;
        if (expiratedtime != null) {
            this.expireTime = expiratedtime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }
}
