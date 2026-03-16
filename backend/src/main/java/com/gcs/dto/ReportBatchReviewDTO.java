package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 举报批量审核请求对象
 */
@Data
@Schema(description = "举报批量审核请求对象")
public class ReportBatchReviewDTO {
    
    @NotEmpty(message = "举报 ID 数组不能为空")
    @Schema(description = "举报 ID 数组", required = true, example = "[1, 2, 3]")
    private Long[] reportIds;
    
    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)", required = true, example = "1")
    private Integer reviewStatus;
    
    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容", required = true, example = "已处理，感谢举报")
    private String replyContent;
}
