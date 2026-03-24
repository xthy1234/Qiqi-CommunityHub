package com.gcs.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 文章创建请求对象（草稿版 - 最小化必填字段）
 */
@Data
@Schema(description = "文章创建请求对象（草稿版）")
public class ArticleDraftDTO {
    
    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题（必填）", example = "我的文章")
    private String title;
    
    @NotNull(message = "内容不能为空")
    @Schema(description = "内容（必填）")
    private Map<String, Object> content;

    @Schema(description = "扩展信息 (JSON 格式)")
    private Map<String, Object> extra;

    @Schema(description = "封面图片 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;
    
    @NotNull(message = "分类 ID 不能为空")
    @Schema(description = "内容分类 ID（必填）", example = "1")
    private Long categoryId;
    
    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;
}
