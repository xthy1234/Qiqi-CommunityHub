package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 草稿简略信息 VO（用于列表展示）
 */
@Data
@Schema(description = "草稿简略信息")
public class ArticleDraftSimpleVO {
    
    @Schema(description = "草稿 ID")
    private Long id;
    
    @Schema(description = "文章 ID")
    private Long articleId;
    
    @Schema(description = "草稿标题")
    private String title;
    
    @Schema(description = "封面图片 URL")
    private String coverUrl;
    
    @Schema(description = "内容分类 ID")
    private Long categoryId;
    
    @Schema(description = "最后自动保存时间")
    private LocalDateTime autoSavedAt;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
