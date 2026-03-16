package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 互动统计视图对象
 */
@Data
@Schema(description = "互动统计视图对象")
public class InteractionStatisticsVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "总互动数", example = "100")
    private Integer totalCount;
    
    @Schema(description = "点赞数", example = "80")
    private Integer likeCount;
    
    @Schema(description = "点踩数", example = "5")
    private Integer dislikeCount;
    
    @Schema(description = "收藏数", example = "10")
    private Integer favoriteCount;
    
    @Schema(description = "关注数", example = "5")
    private Integer followCount;
    
    @Schema(description = "竞拍参与数", example = "0")
    private Integer auctionCount;
    
    @Schema(description = "今日新增互动数", example = "15")
    private Integer todayCount;
}
