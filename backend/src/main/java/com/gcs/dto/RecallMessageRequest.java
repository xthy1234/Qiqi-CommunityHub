package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * WebSocket 撤回消息传输对象
 */
@Data
@Schema(description = "撤回消息请求")
public class RecallMessageRequest {
    
    @Schema(description = "消息 ID", example = "789")
    private Long messageId;
    
    @Schema(description = "操作用户 ID", example = "123")
    private Long userId;
    
    @Schema(description = "撤回原因（可选）", example = "发错了")
    private String reason;
}
