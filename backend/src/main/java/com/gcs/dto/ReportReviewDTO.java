package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 举报审核请求对象
 */
@Data
@Schema(description = "举报审核请求对象")
public class ReportReviewDTO {
    
    @NotNull(message = "举报 ID 不能为空")
    @Schema(description = "举报 ID", required = true, example = "1")
    private Long reportId;
    
    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)", required = true, example = "1")
    private Integer reviewStatus;
    
    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容", required = true, example = "已处理，感谢举报")
    private String replyContent;
}
