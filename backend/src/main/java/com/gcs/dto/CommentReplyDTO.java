package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论回复请求对象
 */
@Data
@Schema(description = "评论回复请求对象")
public class CommentReplyDTO {
    
    @NotNull(message = "关联内容 ID 不能为空")
    @Schema(description = "关联内容 ID（文章ID）", required = true, example = "19")
    private Long contentId;
    
    @Schema(description = "被回复的用户 ID（用于显示'回复 @xxx'）", example = "3")
    private Long replyId;
    
    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容", required = true, example = "这是回复内容")
    private String replyContent;
}
