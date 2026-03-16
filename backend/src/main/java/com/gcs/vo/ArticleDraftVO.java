package com.gcs.vo;

import com.gcs.enums.AuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章草稿数据传输对象（用于返回草稿详情）
 */
@Data
@Schema(description = "文章草稿数据")
public class ArticleDraftVO {
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "标题", example = "我的文章")
    private String title;
    
    @Schema(description = "内容详情", example = "这是文章内容...")
    private String content;
    
    @Schema(description = "封面图片 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;
    
    @Schema(description = "分类 ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "分类名称", example = "技术交流")
    private String categoryName;
    
    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;
    
    @Schema(description = "作者 ID", example = "1")
    private Long authorId;
    
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿)", example = "3")
    private AuditStatus auditStatus;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    private LocalDateTime updateTime;
}
