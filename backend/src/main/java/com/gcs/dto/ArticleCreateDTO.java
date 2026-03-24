package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 帖子创建请求对象（正式版 - 需要完整信息）
 */
@Data
@Schema(description = "帖子创建请求对象（正式版）")
public class ArticleCreateDTO {
    
    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", required = true, example = "我的第一篇帖子")
    private String title;
    
    @NotNull(message = "分类 ID 不能为空")
    @Schema(description = "内容分类 ID", required = true, example = "1")
    private Long categoryId;
    
    @Schema(description = "封面图片 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;
    
    @Schema(description = "内容详情")
    private Map<String, Object> content;

    @Schema(description = "扩展信息 (JSON格式)")
    private Map<String, Object> extra;

    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;
}
