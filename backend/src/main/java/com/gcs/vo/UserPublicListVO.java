package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户公开列表 VO（用于分页展示，仅包含安全字段）
 */
@Data
@Schema(description = "用户公开列表 VO")
public class UserPublicListVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "用户 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户昵称", example = "张三")
    private String nickname;
    
    @Schema(description = "头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @Schema(description = "性别 (0:保密，1:男，2:女)", example = "1")
    private Integer gender;
    
    @Schema(description = "个人签名", example = "生活不止眼前的苟且")
    private String signature;
    
    @Schema(description = "粉丝数量", example = "100")
    private Integer followerCount;
    
    @Schema(description = "关注数量", example = "50")
    private Integer followingCount;
    
    @Schema(description = "文章数量", example = "20")
    private Integer articleCount;
    
    @Schema(description = "当前用户是否已关注", example = "true")
    private Boolean isFollowing;
    
    @Schema(description = "是否为当前登录用户自己", example = "false")
    private Boolean isSelf;
    
    @Schema(description = "注册时间", example = "2026-04-09 13:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
