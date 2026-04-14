package com.gcs.dto;

import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理员创建用户请求对象
 */
@Data
@Schema(description = "管理员创建用户请求对象")
public class AdminUserCreateDTO {
    
    @NotBlank(message = "账号不能为空")
    @Schema(description = "用户账号", required = true, example = "user123")
    private String account;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "密码只能包含字母和数字")
    @Schema(description = "用户密码", required = true, example = "123456")
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称长度不能超过 20 个字符")
    @Schema(description = "用户昵称", required = true, example = "张三")
    private String nickname;
    
    @Schema(description = "性别 (0:保密，1:男，2:女)", example = "0")
    private Integer gender;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "电话号码", example = "13800138000")
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "user@example.com")
    private String email;
    
    @NotNull(message = "角色 ID 不能为空")
    @Schema(description = "角色 ID", required = true, example = "1")
    private Long roleId;
    
    @Schema(description = "生日", example = "2000-01-01")
    private LocalDate birthday;
    
    @Schema(description = "个人签名", example = "生活不止眼前的苟且，还有诗和远方")
    private String signature;
    
    @Schema(description = "头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private CommonStatus status = CommonStatus.ENABLED;
}
