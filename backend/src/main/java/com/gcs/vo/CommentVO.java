package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论基础视图对象 - 用于列表展示
 */
@Data
@Schema(description = "评论基础视图对象")
public class CommentVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "关联内容 ID", example = "1")
    private Long contentId;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户头像 URL", example = "http://example.com/avatar.jpg")
    private String userAvatar;
    
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;
    
    @Schema(description = "评论内容", example = "这是一条测试评论")
    private String content;
    
    @Schema(description = "父级评论 ID", example = "0")
    private Long parentId;
    
    @Schema(description = "点赞数", example = "10")
    private Integer likeCount;
    
    @Schema(description = "状态（0:显示，1:隐藏）", example = "0")
    private CommentStatus status;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
