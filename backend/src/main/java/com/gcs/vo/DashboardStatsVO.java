package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘统计数据 VO
 */
@Data
@Schema(description = "仪表盘统计数据 VO")
public class DashboardStatsVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "总用户数", example = "1000")
    private Integer totalUsers;
    
    @Schema(description = "今日新增用户数", example = "50")
    private Integer todayNewUsers;
    
    @Schema(description = "昨日新增用户数", example = "45")
    private Integer yesterdayNewUsers;
    
    @Schema(description = "用户日环比增长率 (%)", example = "11.11")
    private Double userGrowthRate;
    
    @Schema(description = "总文章数", example = "500")
    private Integer totalArticles;
    
    @Schema(description = "今日新增文章数", example = "30")
    private Integer todayNewArticles;
    
    @Schema(description = "昨日新增文章数", example = "25")
    private Integer yesterdayNewArticles;
    
    @Schema(description = "文章日环比增长率 (%)", example = "20.0")
    private Double articleGrowthRate;
    
    @Schema(description = "待审核文章数", example = "10")
    private Integer pendingAuditArticles;
    
    @Schema(description = "待处理举报数", example = "5")
    private Integer pendingReports;
    
    @Schema(description = "总评论数", example = "2000")
    private Integer totalComments;
    
    @Schema(description = "今日活跃用户数 (DAU)", example = "200")
    private Integer todayActiveUsers;
}
