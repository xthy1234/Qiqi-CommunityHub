package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 圈子聊天会话 VO（用于会话列表展示）
 */
@Data
@Schema(description = "圈子聊天会话视图对象")
public class CircleChatSessionVO {

    @Schema(description = "圈子 ID", example = "123")
    private Long circleId;

    @Schema(description = "圈子名称", example = "游戏战队")
    private String circleName;

    @Schema(description = "圈子头像", example = "http://example.com/circle.jpg")
    private String circleAvatar;

    @Schema(description = "最后一条消息 ID", example = "789")
    private Long lastMessageId;

    @Schema(description = "最后一条消息内容", example = "晚上一起开黑吗？")
    private String lastMessageContent;

    @Schema(description = "最后一条消息发送者 ID", example = "456")
    private Long lastMessageSenderId;

    @Schema(description = "最后一条消息发送者昵称", example = "李四")
    private String lastMessageSenderNickname;

    @Schema(description = "最后一条消息时间", example = "2024-01-01 12:30:00")
    private LocalDateTime lastMessageTime;

    @Schema(description = "未读消息数", example = "5")
    private Integer unreadCount;

    @Schema(description = "成员数量", example = "50")
    private Integer memberCount;
}
