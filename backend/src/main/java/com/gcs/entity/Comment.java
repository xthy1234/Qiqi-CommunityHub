package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.CommentStatus;
import com.gcs.handler.CommentStatusTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 评论实体类
 * 数据库通用操作实体类（普通增删改查）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论实体")
@TableName("comment")
public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联内容 ID（帖子 ID 等）
     */
    @Schema(description = "关联内容 ID（帖子 ID 等）", example = "1")
    private Long contentId;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /**
     * 用户头像 URL
     */
    @Schema(description = "用户头像 URL", example = "http://example.com/avatar.jpg")
    private String userAvatar;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容", example = "这是一条测试评论")
    private String content;

    /**
     * 回复内容
     */
    @Schema(description = "回复内容", example = "这是回复内容")
    private String replyContent;

    /**
     * 父级评论 ID（用于嵌套评论）
     */
    @Schema(description = "父级评论 ID（用于嵌套评论）", example = "0")
    private Long parentId;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "10")
    private Integer likeCount = 0;

    /**
     * 状态（0:显示，1:隐藏）
     */
    @Schema(description = "状态 (0:显示，1:隐藏)", example = "0")
    @TableField(typeHandler = CommentStatusTypeHandler.class)
    private CommentStatus status = CommentStatus.SHOW;
    
    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean isDeleted = false;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
