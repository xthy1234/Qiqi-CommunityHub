package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.CommonStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统配置实体类
 * 用于存储系统级别的配置信息
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统配置实体")
@TableName("config")
public class Config implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键名
     */
    @Schema(description = "配置键名", example = "site.name")
    private String configKey;

    /**
     * 配置值
     */
    @Schema(description = "配置值", example = "游戏社区平台")
    private String configValue;

    /**
     * 配置描述
     */
    @Schema(description = "配置描述", example = "网站名称")
    private String description;

    /**
     * 配置类型（system:系统配置，business:业务配置）
     */
    @Schema(description = "配置类型 (system:系统配置，business:业务配置)", example = "system")
    private String configType = "system";

    /**
     * 状态（0:启用，1:禁用）
     */
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private CommonStatus status = CommonStatus.ENABLED;
    
    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean isDeleted = false;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
