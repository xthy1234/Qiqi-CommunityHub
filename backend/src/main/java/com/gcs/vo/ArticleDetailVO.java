package com.gcs.vo;

import com.gcs.enums.AuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子详情视图对象 - 包含完整信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "帖子详情视图对象")
public class ArticleDetailVO extends ArticleVO {
    
    @Schema(description = "内容详情", example = "这是帖子的具体内容...")
    private String content;
    
    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;
    
    @Schema(description = "收藏数量", example = "10")
    private Integer favoriteCount;
    
    @Schema(description = "审核回复内容", example = "审核通过")
    private String auditReply;
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    private String updateTime;
}
