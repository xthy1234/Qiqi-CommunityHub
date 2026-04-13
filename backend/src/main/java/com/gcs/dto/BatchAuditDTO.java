package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量审核请求对象
 */
@Data
@Schema(description = "批量审核请求")
public class BatchAuditDTO {
    
    @NotNull(message = "文章 ID 列表不能为空")
    @Schema(description = "文章 ID 数组", required = true, example = "[1, 2, 3]")
    private Long[] ids;
    
    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态（0:待审核，1:通过，2:拒绝）", required = true, example = "1")
    private Integer status;
    
    @Schema(description = "审核回复（可选）", example = "内容质量优秀")
    private String reply;
}
