package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水视图对象
 */
@Data
@Schema(description = "积分流水视图对象")
public class PointsTransactionVO {
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户信息")
    private UserSimpleVO user;
    
    @Schema(description = "变动金额", example = "10")
    private Integer amount;
    
    @Schema(description = "变动后余额", example = "100")
    private Integer balance;
    
    @Schema(description = "来源", example = "sign_in")
    private String source;
    
    @Schema(description = "来源描述", example = "每日签到")
    private String description;
    
    @Schema(description = "关联业务 ID", example = "123")
    private Long sourceId;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
}
