package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章审核历史 VO
 */
@Data
@Schema(description = "文章审核历史 VO")
public class ArticleAuditHistoryVO {
    
    @Schema(description = "审核记录 ID", example = "1")
    private Long id;
    
    @Schema(description = "文章 ID", example = "5")
    private Long articleId;
    
    @Schema(description = "审核员 ID", example = "2")
    private Long auditorId;
    
    @Schema(description = "审核员昵称", example = "管理员")
    private String auditorNickname;
    
    @Schema(description = "原审核状态", example = "0")
    private Integer oldStatus;
    
    @Schema(description = "新审核状态", example = "1")
    private Integer newStatus;
    
    @Schema(description = "审核原因/备注", example = "内容优质，通过审核")
    private String reason;
    
    @Schema(description = "审核时间", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditTime;
}
