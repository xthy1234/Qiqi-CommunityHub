package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 反馈状态更新请求对象
 */
@Data
@Schema(description = "反馈状态更新请求对象")
public class FeedbackStatusUpdateDTO {
    
    @NotNull(message = "反馈 ID 不能为空")
    @Schema(description = "反馈 ID", required = true, example = "1")
    private Long feedbackId;
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态 (0:待处理，1:已回复，2:已关闭)", required = true, example = "1")
    private Integer status;
}
