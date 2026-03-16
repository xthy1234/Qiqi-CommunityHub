package com.gcs.dto;

import com.gcs.enums.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论通用数据传输对象
 */
@Data
@Schema(description = "评论数据传输对象")
public class CommentDTO {
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @NotNull(message = "关联内容 ID 不能为空")
    @Schema(description = "关联内容 ID（帖子 ID 等）", required = true, example = "1")
    private Long contentId;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户头像 URL", example = "http://example.com/avatar.jpg")
    private String userAvatar;
    
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;
    
    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", required = true, example = "这是一条测试评论")
    private String content;
    
    @Schema(description = "回复内容", example = "这是回复内容")
    private String replyContent;
    
    @Schema(description = "父级评论 ID（用于嵌套评论）", example = "0")
    private Long parentId;
    
    @Schema(description = "点赞数", example = "10")
    private Integer likeCount;
    
    @Schema(description = "状态（0:显示，1:隐藏）", example = "0")
    private CommentStatus status;
}
