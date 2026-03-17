package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 邀请成员请求 DTO
 */
@Data
@Schema(description = "邀请成员请求对象")
public class CircleInviteDTO {
    
    @NotNull(message = "用户 ID 不能为空")
    @Schema(description = "被邀请用户 ID", example = "123", required = true)
    private Long userId;
}
