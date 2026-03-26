package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;


import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 通知实体类
 * 记录用户的系统通知（回复、点赞、关注、消息等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知实体")
@TableName(value = "notification", autoResultMap = true)
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /**
     * 通知类型（1:回复，2:点赞，3:关注，4:私聊消息，5:圈子更新，6:圈子聊天@，7:提交建议，8:建议审核结果）
     */
    @Schema(description = "通知类型（1:回复，2:点赞，3:关注，4:私聊消息，5:圈子更新，6:圈子聊天@，7:提交建议，8:建议审核结果）", example = "1")
    private Integer type;

    /**
     * 关联的业务 ID（如评论 ID、文章 ID、消息 ID 等）
     */
    @Schema(description = "关联的业务 ID", example = "100")
    private Long sourceId;

    /**
     * 通知简要内容
     */
    @Schema(description = "通知简要内容")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;

    /**
     * 通知额外内容
     */
    @Schema(description = "通知额外内容")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;

    /**
     * 是否已读（false:未读，true:已读）
     */
    @Schema(description = "是否已读", example = "false")
    private Boolean isRead;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
