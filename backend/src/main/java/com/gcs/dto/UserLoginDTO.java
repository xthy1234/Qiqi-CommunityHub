package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求对象
 */
@Data
@Schema(description = "用户登录请求对象")
public class UserLoginDTO {
    
    @NotBlank(message = "账号不能为空")
    @Schema(description = "用户账号", required = true, example = "user@example.com")
    private String account;
    
    @NotBlank(message = "密码不能为空")
    @Schema(description = "用户密码", required = true, example = "password123")
    private String password;
    
    @Schema(description = "验证码（管理员登录时需要）", example = "ABCD")
    private String captcha;
}
