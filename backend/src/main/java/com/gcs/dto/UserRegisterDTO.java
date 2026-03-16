package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求对象
 */
@Data
@Schema(description = "用户注册请求对象")
public class UserRegisterDTO {
    
    @NotBlank(message = "账号不能为空")
    @Schema(description = "用户账号", required = true, example = "user123")
  private String account;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "密码只能包含字母和数字")
    @Schema(description = "用户密码", required = true, example = "password123")
  private String password;
    
    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "电话号码", required = true, example = "13800138000")
  private String phone;
    
    @Size(max = 20, message = "昵称长度不能超过 20 个字符")
    @Schema(description = "用户昵称（可选，为空则使用账号名）", example = "张三")
  private String nickname;
    
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址（可选）", example = "user@example.com")
  private String email;
    
    @Schema(description = "头像 URL（可选，为空则使用默认头像）", example = "http://example.com/avatar.jpg")
  private String avatar;
}
