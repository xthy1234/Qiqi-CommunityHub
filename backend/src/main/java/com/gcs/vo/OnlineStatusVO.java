package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 在线状态变更通知 VO
 */
@Data
@Schema(description = "在线状态变更通知")
public class OnlineStatusVO {
    
    @Schema(description = "用户 ID", example = "123")
    private Long userId;
    
    @Schema(description = "是否在线", example = "true")
    private Boolean isOnline;
    
    @Schema(description = "上线/下线时间", example = "2024-01-01 12:00:00")
    private Long timestamp;
}
