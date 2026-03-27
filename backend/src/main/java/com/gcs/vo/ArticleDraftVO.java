package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "文章草稿视图对象")
public class ArticleDraftVO {
    
    @Schema(description = "草稿 ID")
    private Long id;
    
    @Schema(description = "文章 ID")
    private Long articleId;
    
    @Schema(description = "草稿作者 ID")
    private Long userId;
    
    @Schema(description = "草稿作者信息")
    private UserSimpleVO author;
    
    @Schema(description = "草稿内容（JSONB）")
    private Map<String, Object> content;
    
    @Schema(description = "草稿标题")
    private String title;
    
    @Schema(description = "修改摘要")
    private String changeSummary;
    
    @Schema(description = "最后自动保存时间")
    private LocalDateTime autoSavedAt;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
