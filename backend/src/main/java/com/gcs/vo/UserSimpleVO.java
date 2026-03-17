package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户简易信息 VO（用于消息列表）
 */
@Data
@Schema(description = "用户简易信息")
public class UserSimpleVO {
    
    @Schema(description = "用户 ID", example = "123")
    private Long id;
    
    @Schema(description = "用户昵称", example = "张三")
    private String nickname;
    
    @Schema(description = "头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
}
