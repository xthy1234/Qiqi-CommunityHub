package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.MessageStatus;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 圈子聊天记录实体类
 */
@Data
@TableName("circle_chat")
public class CircleChat implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 圈子 ID
     */
    @Schema(description = "圈子 ID", example = "1")
    private Long circleId;

    /**
     * 发送方用户 ID
     */
    @Schema(description = "发送方用户 ID", example = "1")
    private Long senderId;

    /**
     * 消息内容 (JSON格式)
     */
    @Schema(description = "消息内容 (JSON格式)")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;

    /**
     * 扩展信息 (JSON格式)
     */
    @Schema(description = "扩展信息 (JSON格式)")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;

    /**
     * 消息类型 (0:文本，1:图片，2:文件)
     */
    @Schema(description = "消息类型 (0:文本，1:图片，2:文件)", example = "0")
    private Integer msgType;

    /**
     * 消息状态 (0:未读，1:已读)
     */
    @Schema(description = "消息状态 (0:未读，1:已读)", example = "0")
    private Integer status = 0;

    /**
     * 创建时间
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;



    /**
     * 是否已撤回
     */
    @Schema(description = "是否已撤回", example = "false")
    private Boolean isRecalled = false;

    /**
     * 是否被管理员删除
     */
    @Schema(description = "是否被管理员删除", example = "false")
    private Boolean deletedByAdmin = false;

    /**
     * 删除者用户 ID（管理员或圈主）
     */
    @Schema(description = "删除者用户 ID（管理员或圈主）", example = "1")
    private Long deletedBy;

    /**
     * 删除时间
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("deleted_time")
    private LocalDateTime deletedTime;
}
