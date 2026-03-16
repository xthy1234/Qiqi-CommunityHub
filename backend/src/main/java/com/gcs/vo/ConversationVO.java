package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项 VO
 */
@Data
@Schema(description = "会话列表项")
public class ConversationVO {
    
    @Schema(description = "对方用户 ID", example = "456")
    private Long userId;
    
    @Schema(description = "对方用户名/昵称", example = "张三")
    private String username;
    
    @Schema(description = "对方头像", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @Schema(description = "最后一条消息内容", example = "你好")
    private String lastMessage;
    
    @Schema(description = "最后一条消息时间", example = "2024-01-01 12:00:00")
    private LocalDateTime lastTime;
    
    @Schema(description = "未读消息数", example = "3")
    private Integer unreadCount;
}
