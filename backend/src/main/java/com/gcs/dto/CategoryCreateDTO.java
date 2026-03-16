package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类创建请求对象
 */
@Data
@Schema(description = "分类创建请求对象")
public class CategoryCreateDTO {
    
    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", required = true, example = "技术交流")
    private String categoryName;
    
    @Schema(description = "分类描述", example = "技术相关文章和讨论")
    private String description;
    
    @NotNull(message = "排序字段不能为空")
    @Schema(description = "排序字段", required = true, example = "1")
    private Integer sort;
}
