package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 反馈创建请求对象
 */
@Data
@Schema(description = "反馈创建请求对象")
public class FeedbackCreateDTO {
    
    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容", required = true, example = "这个功能很好用，希望能继续优化")
    private String content;
}
