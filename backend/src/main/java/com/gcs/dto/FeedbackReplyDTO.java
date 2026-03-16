package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 反馈回复请求对象
 */
@Data
@Schema(description = "反馈回复请求对象")
public class FeedbackReplyDTO {
    
    @NotNull(message = "反馈 ID 不能为空")
    @Schema(description = "反馈 ID", required = true, example = "1")
    private Long feedbackId;
    
    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容", required = true, example = "感谢您的反馈，我们会继续努力改进")
    private String replyContent;
}
