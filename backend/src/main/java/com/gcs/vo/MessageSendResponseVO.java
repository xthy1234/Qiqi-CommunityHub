package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发送私信响应 VO
 */
@Data
@Schema(description = "发送私信响应")
public class MessageSendResponseVO {
    
    @Schema(description = "消息 ID", example = "789")
    private Long messageId;
    
    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    private LocalDateTime createTime;
}
