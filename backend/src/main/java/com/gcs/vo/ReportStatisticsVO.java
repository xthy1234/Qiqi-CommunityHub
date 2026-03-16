package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 举报统计视图对象
 */
@Data
@Schema(description = "举报统计视图对象")
public class ReportStatisticsVO {
    
    @Schema(description = "总数量", example = "100")
    private Integer totalCount;
    
    @Schema(description = "待审核数量", example = "20")
    private Integer pendingCount;
    
    @Schema(description = "已通过数量", example = "70")
    private Integer approvedCount;
    
    @Schema(description = "已拒绝数量", example = "10")
    private Integer rejectedCount;
    
    @Schema(description = "有效数量", example = "90")
    private Integer validCount;
    
    @Schema(description = "无效数量", example = "10")
    private Integer invalidCount;
}
