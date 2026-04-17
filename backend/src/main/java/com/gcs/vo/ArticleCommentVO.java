package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章评论列表视图对象（包含主评论 + 前3条高赞子评论）
 */
@Data
@Schema(description = "文章评论列表视图对象")
public class ArticleCommentVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "评论 ID", example = "1")
    private Long id;
    
    @Schema(description = "评论内容", example = "这是一条评论")
    private String content;
    
    @Schema(description = "评论者信息")
    private UserSimpleVO user;
    
    @Schema(description = "被回复的用户 ID", example = "2")
    private Long replyId;
    
    @Schema(description = "被回复的用户信息（用于显示 @xxx）")
    private UserSimpleVO replyToUser;
    
    @Schema(description = "点赞数", example = "10")
    private Integer likeCount = 0;
    
    @Schema(description = "点踩数", example = "2")
    private Integer dislikeCount = 0;
    
    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间", example = "2024-01-01 13:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @Schema(description = "是否已编辑", example = "false")
    private Boolean isEdited = false;
    
    @Schema(description = "子评论总数", example = "8")
    private Integer replyCount = 0;
    
    @Schema(description = "前3条高赞子评论")
    private List<ArticleCommentVO> topReplies;

    @Schema(description = "是否点赞", example = "false")
    private Boolean isLiked = false;

    @Schema(description = "是否点踩", example = "false")
    private Boolean isDisliked = false;
}
