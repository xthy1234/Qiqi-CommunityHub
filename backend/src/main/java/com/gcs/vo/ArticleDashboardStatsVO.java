package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 管理后台统计数据 VO
 */
@Data
@Schema(description = "管理后台统计数据 VO")
public class ArticleDashboardStatsVO {
    
    @Schema(description = "今日新增文章数", example = "10")
    private Integer todayCount;
    
    @Schema(description = "昨日新增文章数", example = "8")
    private Integer yesterdayCount;
    
    @Schema(description = "待审核文章数", example = "5")
    private Integer pendingCount;
    
    @Schema(description = "已通过文章数", example = "150")
    private Integer approvedCount;
    
    @Schema(description = "已拒绝文章数", example = "3")
    private Integer rejectedCount;
    
    @Schema(description = "总文章数", example = "158")
    private Integer totalCount;
    
    @Schema(description = "总浏览量", example = "10240")
    private Integer totalViewCount;
    
    @Schema(description = "热门文章 TOP 10", example = "[]")
    private Object topArticles;
    
    @Schema(description = "活跃作者 TOP 10", example = "[]")
    private Object topAuthors;
    
    @Schema(description = "分类统计", example = "{}")
    private Map<String, Integer> categoryStats;
    
    @Schema(description = "日环比增长率 (%)", example = "25.0")
    private Double dayOverDayGrowth;
}
