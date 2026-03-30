package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户积分信息 DTO
 */
@Data
@Schema(description = "用户积分信息")
public class UserPointsDTO {
    
    @Schema(description = "当前积分", example = "1280")
    private Integer points;
    
    @Schema(description = "连续签到天数", example = "7")
    private Integer streak;
    
    @Schema(description = "今日是否已签到", example = "false")
    private Boolean signedInToday;
}
