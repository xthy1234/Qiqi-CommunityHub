package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文章审核概览统计数据 VO
 */
@Data
@Schema(description = "文章审核概览统计数据 VO")
public class ArticleAuditOverviewVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "待审核文章数", example = "10")
    private Integer pendingCount;
    
    @Schema(description = "今日通过文章数", example = "25")
    private Integer todayApproved;
    
    @Schema(description = "今日拒绝文章数", example = "5")
    private Integer todayRejected;
    
    @Schema(description = "总文章数", example = "500")
    private Integer totalCount;
}
