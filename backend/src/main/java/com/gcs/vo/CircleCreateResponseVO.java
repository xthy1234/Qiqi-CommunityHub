package com.gcs.vo;

import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建圈子响应视图对象
 */
@Data
@Schema(description = "创建圈子响应视图对象")
public class CircleCreateResponseVO {
    
    @Schema(description = "圈子 ID", example = "1")
    private Long id;
    
    @Schema(description = "圈子名称", example = "游戏战队")
    private String name;
    
    @Schema(description = "圈子描述", example = "欢迎加入我们的游戏战队")
    private String description;
    
    @Schema(description = "圈子头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @Schema(description = "圈主 ID", example = "1")
    private Long ownerId;
    
    @Schema(description = "圈子类型 (0:私密，1:公开，2:个人空间)", example = "1")
    private Integer type;
    
    @Schema(description = "创建时间", example = "2026-03-17 12:00:00")
    private String createTime;

    @Schema(description = "状态", example = "0")
    private CommonStatus status;
}
