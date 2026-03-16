package com.gcs.dto;

import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统配置创建请求对象
 */
@Data
@Schema(description = "系统配置创建请求对象")
public class ConfigCreateDTO {
    
    @NotBlank(message = "配置键名不能为空")
    @Schema(description = "配置键名", required = true, example = "site.name")
    private String configKey;
    
    @NotBlank(message = "配置值不能为空")
    @Schema(description = "配置值", required = true, example = "游戏社区平台")
    private String configValue;
    
    @Schema(description = "配置描述", example = "网站名称")
    private String description;
    
    @NotNull(message = "配置类型不能为空")
    @Schema(description = "配置类型 (system:系统配置，business:业务配置)", required = true, example = "system")
    private String configType;
    
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private CommonStatus status;
}
