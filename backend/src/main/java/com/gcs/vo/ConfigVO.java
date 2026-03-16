package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置基础视图对象
 */
@Data
@Schema(description = "系统配置基础视图对象")
public class ConfigVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "配置键名", example = "site.name")
    private String configKey;
    
    @Schema(description = "配置值", example = "游戏社区平台")
    private String configValue;
    
    @Schema(description = "配置描述", example = "网站名称")
    private String description;
    
    @Schema(description = "配置类型 (system:系统配置，business:业务配置)", example = "system")
    private String configType;
    
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private CommonStatus status;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
