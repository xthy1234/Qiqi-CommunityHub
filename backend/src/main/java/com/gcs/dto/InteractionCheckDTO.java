package com.gcs.dto;

import com.gcs.enums.InteractionActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 检查互动状态请求对象
 */
@Data
@Schema(description = "检查互动状态请求对象")
public class InteractionCheckDTO {
    
    @NotNull(message = "内容 ID 不能为空")
    @Schema(description = "内容 ID", required = true, example = "1")
    private Long contentId;
    
    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作类型", required = true, example = "21")
    private InteractionActionType actionType;
}
