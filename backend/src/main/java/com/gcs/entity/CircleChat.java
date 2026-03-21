package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子聊天消息实体
 * @author 
 * @date 2026-03-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("circle_chat")
public class CircleChat implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 圈子 ID
     */
    private Long circleId;

    /**
     * 发送方用户 ID
     */
    private Long senderId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型 (0:文本，1:图片，2:文件)
     */
    private Integer msgType;

    /**
     * 消息状态 (0:未读，1:已读)
     */
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
    private Boolean isRecalled = false;

    /**
     * 是否被管理员删除
     */
    @TableField("deleted_by_admin")
    private Boolean deletedByAdmin = false;

    /**
     * 删除者用户 ID（管理员或圈主）
     */
    @TableField("deleted_by")
    private Long deletedBy;

    /**
     * 删除时间
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("deleted_time")
    private LocalDateTime deletedTime;
}
