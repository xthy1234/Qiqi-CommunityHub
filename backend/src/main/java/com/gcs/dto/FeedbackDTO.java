package com.gcs.dto;

import com.gcs.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈通用数据传输对象
 */
@Data
@Schema(description = "反馈数据传输对象")
public class FeedbackDTO {
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;
    
    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容", required = true, example = "这个功能很好用，希望能继续优化")
    private String content;
    
    @Schema(description = "回复内容", example = "感谢您的反馈，我们会继续努力改进")
    private String replyContent;
    
    @Schema(description = "回复时间", example = "2026-01-01 12:00:00")
    private LocalDateTime replyTime;
    
    @Schema(description = "状态 (0:待处理，1:已回复，2:已关闭)", example = "0")
    private FeedbackStatus status;
}
