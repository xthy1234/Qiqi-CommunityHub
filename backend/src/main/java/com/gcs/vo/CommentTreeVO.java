package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论树形结构视图对象 - 用于嵌套评论展示
 */
@Data
@Schema(description = "评论树形结构视图对象")
public class CommentTreeVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户头像 URL", example = "http://example.com/avatar.jpg")
    private String userAvatar;
    
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;
    
    @Schema(description = "评论内容", example = "这是一条测试评论")
    private String content;
    
    @Schema(description = "回复内容", example = "这是回复内容")
    private String replyContent;
    
    @Schema(description = "父级评论 ID", example = "0")
    private Long parentId;
    
    @Schema(description = "点赞数", example = "10")
    private Integer likeCount;
    
    @Schema(description = "当前用户是否点赞", example = "true")
    private Boolean isLiked;
    
    @Schema(description = "当前用户是否点踩", example = "false")
    private Boolean isDisliked;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "子评论列表")
    private List<CommentTreeVO> children;
    
    @Schema(description = "评论层级", example = "1")
    private Integer level;
}
