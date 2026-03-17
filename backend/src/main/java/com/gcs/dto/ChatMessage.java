package com.gcs.dto;

import com.gcs.vo.UserSimpleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket 聊天消息传输对象
 */
@Data
@Schema(description = "WebSocket 聊天消息")
public class ChatMessage {
    
    @Schema(description = "消息 ID", example = "1")
    private Long id;
    
    @Schema(description = "发送方用户 ID", example = "123")
    private Long fromUserId;
    
    @Schema(description = "发送方用户信息")
    private UserSimpleVO fromUser;
    
    @Schema(description = "接收方用户 ID", example = "456")
    private Long toUserId;
    
    @Schema(description = "接收方用户信息")
    private UserSimpleVO toUser;
    
    @Schema(description = "消息内容", example = "你好！")
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
    private String action;          // SEND: 发送消息，READ: 已读回执，RECALL: 撤回消息
}
