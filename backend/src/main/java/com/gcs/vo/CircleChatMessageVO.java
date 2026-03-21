package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 圈子聊天消息 VO（RESTful 返回用）
 */
@Data
@Schema(description = "圈子聊天消息视图对象")
public class CircleChatMessageVO {

    @Schema(description = "消息 ID", example = "1")
    private Long id;

    @Schema(description = "圈子 ID", example = "123")
    private Long circleId;

    @Schema(description = "发送方用户 ID", example = "456")
    private Long senderId;

    @Schema(description = "发送方用户信息")
    private com.gcs.vo.UserSimpleVO sender;

    @Schema(description = "消息内容", example = "大家好！")
    private String content;

    @Schema(description = "消息类型 (0:文本，1:图片，2:文件)", example = "0")
    private Integer msgType;

    @Schema(description = "是否已撤回", example = "false")
    private Boolean isRecalled;

    @Schema(description = "是否被管理员删除", example = "false")
    private Boolean deletedByAdmin;

    @Schema(description = "删除者信息")
    private com.gcs.vo.UserSimpleVO deleter;

    @Schema(description = "删除时间", example = "2024-01-01 12:00:00")
    private LocalDateTime deletedTime;

    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "是否为当前用户发送", example = "true")
    private Boolean isSelf;
}
