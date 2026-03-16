package com.gcs.dto;

import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户通用数据传输对象
 */
@Data
@Schema(description = "用户数据传输对象")
public class UserDTO {
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @NotBlank(message = "账号不能为空")
    @Email(message = "账号格式不正确")
    @Schema(description = "用户账号", required = true, example = "user@example.com")
    private String account;
    
    @Schema(description = "用户密码（加密存储）", example = "password123")
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    @Schema(description = "用户昵称", required = true, example = "张三")
    private String nickname;
    
    @Schema(description = "头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @Schema(description = "性别 (0:保密，1:男，2:女)", example = "1")
    private Integer gender;
    
    @Schema(description = "电话号码", example = "13800138000")
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "user@example.com")
    private String email;
    
    @Schema(description = "角色 ID", example = "1")
    private Long roleId;
    
    @Schema(description = "生日", example = "2000-01-01")
    private LocalDate birthday;
    
    @Schema(description = "个人签名", example = "生活不止眼前的苟且，还有诗和远方")
    private String signature;
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态 (0:启用，1:禁用)", required = true, example = "0")
    private CommonStatus status;
}
