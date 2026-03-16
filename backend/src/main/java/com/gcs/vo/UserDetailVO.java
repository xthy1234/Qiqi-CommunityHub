package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户详情视图对象 - 包含完整信息
 * 注意：不包含密码等敏感信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户详情视图对象")
public class UserDetailVO extends UserVO {
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @Schema(description = "最后登录时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    
    @Schema(description = "最后登录 IP", example = "192.168.1.1")
    private String lastLoginIp;
    
    @Schema(description = "角色名称", example = "普通用户")
    private String roleName;
    
    @Schema(description = "文章数量", example = "10")
    private Integer articleCount;
    
    @Schema(description = "评论数量", example = "50")
    private Integer commentCount;
}
