package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户更新头像请求对象
 */
@Data
@Schema(description = "用户更新头像请求对象")
public class UserAvatarDTO {
    
    @NotNull(message = "用户 ID 不能为空")
    @Schema(description = "用户 ID", required = true, example = "1")
    private Long userId;
    
    @NotBlank(message = "头像 URL 不能为空")
    @Schema(description = "头像 URL", required = true, example = "http://example.com/avatar.jpg")
    private String avatarUrl;
}
