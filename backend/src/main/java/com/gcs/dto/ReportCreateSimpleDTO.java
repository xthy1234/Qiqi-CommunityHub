package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 举报创建请求对象（简化版）
 * 只需要内容 ID 和举报原因，其他信息后端自动获取
 */
@Data
@Schema(description = "举报创建请求对象（简化版）")
public class ReportCreateSimpleDTO {
    
    @NotNull(message = "被举报内容 ID 不能为空")
    @Schema(description = "被举报内容 ID", required = true, example = "10")
    private Long contentId;
    
    @NotBlank(message = "举报原因不能为空")
    @Schema(description = "举报原因", required = true, example = "发布虚假信息、恶意灌水等")
    private String reportReason;
    
    @Schema(description = "举报类型", example = "ARTICLE")
    private String reportType; // ARTICLE, COMMENT, USER 等
}
