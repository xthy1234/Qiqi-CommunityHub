package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 反馈批量状态更新请求对象
 */
@Data
@Schema(description = "反馈批量状态更新请求对象")
public class FeedbackBatchStatusUpdateDTO {
    
    @NotEmpty(message = "反馈 ID 数组不能为空")
    @Schema(description = "反馈 ID 数组", required = true, example = "[1, 2, 3]")
    private Long[] ids;
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态 (0:待处理，1:已回复，2:已关闭)", required = true, example = "1")
    private Integer status;
}
