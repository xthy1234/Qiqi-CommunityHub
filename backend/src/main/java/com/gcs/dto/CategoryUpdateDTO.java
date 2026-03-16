package com.gcs.dto;

import com.gcs.enums.CategoryStatus;
import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类更新请求对象
 */
@Data
@Schema(description = "分类更新请求对象")
public class CategoryUpdateDTO {
    
    @Schema(description = "主键 ID", required = true, example = "1")
    private Long id;
    
    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", required = true, example = "技术交流")
    private String categoryName;
    
    @Schema(description = "分类描述", example = "技术相关文章和讨论")
    private String description;
    
    @NotNull(message = "排序字段不能为空")
    @Schema(description = "排序字段", required = true, example = "1")
    private Integer sort;
    
    @Schema(description = "状态（0:启用，1:禁用）", example = "0")
    private CategoryStatus status;
}
