
package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 签到结果 DTO
 */
@Data
@Schema(description = "签到结果")
public class SignInResultDTO {
    
    @Schema(description = "获得积分", example = "10")
    private Integer pointsEarned;
    
    @Schema(description = "连续签到天数", example = "7")
    private Integer streak;
    
    @Schema(description = "提示信息", example = "恭喜！连续签到 7 天，获得额外奖励 20 积分！")
    private String message;
}
