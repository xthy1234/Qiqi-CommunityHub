package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * WebSocket 已读回执传输对象
 */
@Data
@Schema(description = "已读回执")
public class ReadReceipt {
    
    @Schema(description = "当前用户 ID（已读方）", example = "123")
    private Long fromUserId;
    
    @Schema(description = "对方用户 ID（消息发送方）", example = "456")
    private Long toUserId;
    
    @Schema(description = "已读的最后一条消息 ID（可选）", example = "789")
    private Long lastReadMessageId;
}
