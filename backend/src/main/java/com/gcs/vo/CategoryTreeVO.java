package com.gcs.vo;

import com.gcs.enums.CategoryStatus;
import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分类树形结构视图对象
 */
@Data
@Schema(description = "分类树形结构视图对象")
public class CategoryTreeVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "分类名称", example = "技术交流")
    private String categoryName;
    
    @Schema(description = "分类描述", example = "技术相关文章和讨论")
    private String description;
    
    @Schema(description = "排序字段", example = "1")
    private Integer sort;
    
    @Schema(description = "状态（0:启用，1:禁用）", example = "0")
    private CategoryStatus status;
    
    @Schema(description = "子分类列表")
    private List<CategoryTreeVO> children;
}
