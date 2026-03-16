package com.gcs.dto;

import com.gcs.enums.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 评论更新请求对象
 */
@Data
@Schema(description = "评论更新请求对象")
public class CommentUpdateDTO {
    
    @Schema(description = "主键 ID", required = true, example = "1")
    private Long id;
    
    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", required = true, example = "这是一条测试评论")
    private String content;
    
    @Schema(description = "回复内容", example = "这是回复内容")
    private String replyContent;
    
    @Schema(description = "状态（0:显示，1:隐藏）", example = "0")
    private CommentStatus status;
}
