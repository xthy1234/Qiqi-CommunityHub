package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 建议审核 DTO
 */
@Data
@Schema(description = "建议审核数据")
public class SuggestionReviewDTO {

    @NotNull(message = "必须指定审核结果")
    @Schema(description = "是否通过（true-通过，false-拒绝）", example = "true", required = true)
    private Boolean approved;

    @Schema(description = "拒绝理由（仅拒绝时需要）", example = "内容有误")
    private String reason;
}
