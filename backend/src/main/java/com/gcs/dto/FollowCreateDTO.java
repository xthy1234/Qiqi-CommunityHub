package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 关注/取关请求 DTO
 */
@Data
@Schema(description = "关注/取关请求对象")
public class FollowCreateDTO {

    @NotNull(message = "用户ID 不能为空")
    @Schema(description = "被关注/取关的用户ID", example = "123", required = true)
    private Long userId;

    @NotNull(message = "操作类型不能为空")
    @Schema(description = "操作类型：follow-关注，unfollow-取关", example = "follow", required = true)
    private String action;
}
