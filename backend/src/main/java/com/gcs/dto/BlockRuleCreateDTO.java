
package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "屏蔽规则创建请求")
public class BlockRuleCreateDTO {
    @NotBlank(message = "规则类型不能为空")
    @Schema(description = "规则类型 (keyword/category/author)", example = "author")
    private String ruleType;

    @NotBlank(message = "规则值不能为空")
    @Schema(description = "规则值", example = "123")
    private String ruleValue;
}
