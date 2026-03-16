package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 反馈统计视图对象
 */
@Data
@Schema(description = "反馈统计视图对象")
public class FeedbackStatisticsVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "待处理数量", example = "10")
    private Integer pendingCount;
    
    @Schema(description = "已回复数量", example = "50")
    private Integer repliedCount;
    
    @Schema(description = "已关闭数量", example = "30")
    private Integer closedCount;
    
    @Schema(description = "总数量", example = "90")
    private Integer totalCount;
    
    @Schema(description = "今日新增数量", example = "5")
    private Integer todayCount;
    
    @Schema(description = "平均回复时长（小时）", example = "24")
    private Double avgReplyHours;
}
