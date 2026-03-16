package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标记消息已读请求 DTO
 */
@Data
@Schema(description = "标记消息已读请求")
public class MessageReadDTO {
    
    @Schema(description = "消息发送方用户 ID", example = "456")
    private Long fromUserId;
}
