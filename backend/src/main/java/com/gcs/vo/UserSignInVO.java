package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到记录视图对象
 */
@Data
@Schema(description = "签到记录视图对象")
public class UserSignInVO {
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户信息")
    private UserSimpleVO user;
    
    @Schema(description = "签到日期", example = "2026-01-01")
    private LocalDate signDate;
    
    @Schema(description = "获得积分", example = "10")
    private Integer pointsEarned;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createdAt;
}
