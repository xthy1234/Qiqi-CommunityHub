package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 圈子申请审核 DTO
 */
@Data
@Schema(description = "圈子申请审核请求")
public class CircleApplicationReviewDTO {
    
    @NotNull(message = "审核结果不能为空")
    @Schema(description = "是否通过", example = "true")
    private boolean approved;
    
    @Schema(description = "审核备注", example = "欢迎加入")
    private String remark;
}
