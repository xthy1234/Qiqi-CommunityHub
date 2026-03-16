package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录响应对象
 */
@Data
@Schema(description = "用户登录响应对象")
public class UserLoginVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "用户信息")
    private UserDetailVO user;
}
