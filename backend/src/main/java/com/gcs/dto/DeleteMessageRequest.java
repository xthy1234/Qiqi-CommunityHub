package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * WebSocket 删除消息传输对象
 */
@Data
@Schema(description = "删除消息请求")
public class DeleteMessageRequest {
    
    @Schema(description = "消息 ID", example = "789")
    private Long messageId;
    
    @Schema(description = "操作用户 ID", example = "123")
    private Long userId;
    
    @Schema(description = "是否双方都已删除", example = "false")
    private Boolean bothDeleted;
}
