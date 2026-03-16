package com.gcs.dto;

import com.gcs.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * 反馈更新请求对象
 */
@Data
@Schema(description = "反馈更新请求对象")
public class FeedbackUpdateDTO {
    
    @Schema(description = "主键 ID", required = true, example = "1")
    private Long id;
    
    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容", required = true, example = "这个功能很好用，希望能继续优化")
    private String content;
    
    @Schema(description = "状态 (0:待处理，1:已回复，2:已关闭)", example = "0")
    private FeedbackStatus status;
}
