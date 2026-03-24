package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 修改建议提交 DTO
 */
@Data
@Schema(description = "修改建议提交数据")
public class SuggestionSubmitDTO {

    @NotBlank(message = "建议标题不能为空")
    @Schema(description = "建议标题", example = "优化文章结构", required = true)
    private String title;

    @NotNull(message = "建议内容不能为空")
    @Schema(description = "建议内容（JSON）", required = true)
    private Map<String, Object> content;

    @Schema(description = "修改摘要", example = "重新组织了段落")
    private String changeSummary;
}
