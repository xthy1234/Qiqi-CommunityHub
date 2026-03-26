package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 帖子更新请求对象
 */
@Data
@Schema(description = "帖子更新请求对象")
public class ArticleUpdateDTO {
    
    @Schema(description = "主键 ID", required = true, example = "1")
    private Long id;
    
    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", required = true, example = "我的第一篇帖子")
    private String title;
    
    @Schema(description = "封面图片 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;
    
    @NotNull(message = "分类 ID 不能为空")
    @Schema(description = "内容分类 ID", required = true, example = "1")
    private Long categoryId;

    @Schema(description = "内容详情")
    private Map<String, Object> content;

    @Schema(description = "扩展信息 (JSON 格式)")
    private Map<String, Object> extra;

    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;
}
