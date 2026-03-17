package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建圈子 DTO
 */
@Data
@Schema(description = "创建圈子请求对象")
public class CircleCreateDTO {
    
    @NotBlank(message = "圈子名称不能为空")
    @Schema(description = "圈子名称", example = "游戏战队", required = true)
    private String name;
    
    @Schema(description = "圈子描述", example = "欢迎加入我们的游戏战队")
    private String description;
    
    @Schema(description = "圈子头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @NotNull(message = "圈子类型不能为空")
    @Schema(description = "圈子类型 (0:私密，1:公开)", example = "1", required = true)
    private Integer type;
}
