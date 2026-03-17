package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新圈子 DTO
 */
@Data
@Schema(description = "更新圈子请求对象")
public class CircleUpdateDTO {
    
    @Schema(description = "圈子名称", example = "游戏战队")
    private String name;
    
    @Schema(description = "圈子描述", example = "欢迎加入我们的游戏战队")
    private String description;
    
    @Schema(description = "圈子头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @Schema(description = "圈子类型 (0:私密，1:公开)", example = "1")
    private Integer type;
}
