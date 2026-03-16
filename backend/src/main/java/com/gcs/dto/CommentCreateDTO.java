package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论创建请求对象
 */
@Data
@Schema(description = "评论创建请求对象")
public class CommentCreateDTO {
    
    @NotNull(message = "关联内容 ID 不能为空")
    @Schema(description = "关联内容 ID（帖子 ID 等）", required = true, example = "1")
    private Long contentId;
    
    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", required = true, example = "这是一条测试评论")
    private String content;
    
    @Schema(description = "父级评论 ID（用于嵌套评论）", example = "0")
    private Long parentId;
}
