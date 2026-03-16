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
    
    @NotNull(message = "被回复的评论 ID 不能为空")
    @Schema(description = "被回复的评论 ID", required = true, example = "1")
    private Long parentId;
    
    @NotNull(message = "关联内容 ID 不能为空")
    @Schema(description = "关联内容 ID", required = true, example = "1")
    private Long contentId;
    
    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容", required = true, example = "这是回复内容")
    private String replyContent;
}
