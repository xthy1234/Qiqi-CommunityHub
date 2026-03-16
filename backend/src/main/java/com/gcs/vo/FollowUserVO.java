package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注用户信息 VO
 */
@Data
@Schema(description = "关注用户信息视图对象")
public class FollowUserVO {

    @Schema(description = "关注记录 ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "456")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    private String username;

    @Schema(description = "头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "关注时间", example = "2024-01-01T12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime followTime;

    @Schema(description = "个人签名", example = "生活不止眼前的苟且")
    private String signature;

    @Schema(description = "性别 (0:保密，1:男，2:女)", example = "1")
    private Integer gender;
}
