package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 圈子聊天 WebSocket 消息传输对象
 */
@Data
@Schema(description = "圈子聊天 WebSocket 消息")
public class CircleChatMessage {

    @Schema(description = "消息 ID", example = "1")
    private Long id;

    @Schema(description = "圈子 ID", example = "123")
    private Long circleId;

    @Schema(description = "发送方用户 ID", example = "456")
    private Long senderId;

    @Schema(description = "发送方用户信息")
    private UserSimpleVO sender;

    @Schema(description = "消息内容", example = "大家好！")
    private String content;

    @Schema(description = "消息类型 (0:文本，1:图片，2:文件)", example = "0")
    private Integer msgType;

    @Schema(description = "消息状态 (0:未读，1:已读)", example = "0")
    private Integer status;

    @Schema(description = "是否为自己发送的消息", example = "true")
    private Boolean isSelf;

    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "动作类型", example = "SEND")
    private String action;          // SEND: 发送消息，RECALL: 撤回消息，DELETE: 删除消息
}
