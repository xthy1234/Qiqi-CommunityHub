package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统配置详情视图对象 - 包含完整信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统配置详情视图对象")
public class ConfigDetailVO extends ConfigVO {
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    private LocalDateTime updateTime;
}
