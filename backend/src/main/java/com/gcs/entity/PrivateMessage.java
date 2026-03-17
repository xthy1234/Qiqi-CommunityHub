package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.MessageStatus;
import com.gcs.handler.MessageStatusTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 私信实体类
 * @author 
 * @date 2026-03-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "私信实体")
@TableName("private_message")
public class PrivateMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送方用户 ID
     */
    @Schema(description = "发送方用户 ID", example = "123")
    private Long fromUserId;

    /**
     * 接收方用户 ID
     */
    @Schema(description = "接收方用户 ID", example = "456")
    private Long toUserId;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容", example = "你好！")
    private String content;

    /**
     * 消息类型 (0:文本，1:图片，2:文件)
     */
    @Schema(description = "消息类型 (0:文本，1:图片，2:文件)", example = "0")
    private Integer msgType;

    /**
     * 消息状态 (0:未读，1:已读)
     */
    @Schema(description = "消息状态 (0:未读，1:已读)", example = "0")
    @TableField(typeHandler = MessageStatusTypeHandler.class)
    private MessageStatus status = MessageStatus.UNREAD;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 发送方是否删除
     */
    @Schema(description = "发送方是否删除", example = "false")
    private Boolean deletedBySender = false;
    
    /**
     * 接收方是否删除
     */
    @Schema(description = "接收方是否删除", example = "false")
    private Boolean deletedByRecipient = false;
    
    /**
     * 是否已撤回
     */
    @Schema(description = "是否已撤回", example = "false")
    private Boolean isRecalled = false;
}
