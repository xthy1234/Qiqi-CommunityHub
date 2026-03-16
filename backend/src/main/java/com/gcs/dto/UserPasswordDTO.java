package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改密码请求对象
 */
@Data
@Schema(description = "用户修改密码请求对象")
public class UserPasswordDTO {
    
    @NotNull(message = "用户 ID 不能为空")
    @Schema(description = "用户 ID", required = true, example = "1")
    private Long userId;
    
    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码", required = true, example = "password123")
    private String oldPassword;
    
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    @Schema(description = "新密码", required = true, example = "newpassword456")
    private String newPassword;
}
