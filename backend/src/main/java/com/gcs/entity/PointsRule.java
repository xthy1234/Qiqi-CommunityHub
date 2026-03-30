package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 积分规则实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "积分规则实体")
@TableName("points_rule")
public class PointsRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则代码（sign_in, post_article, like_received 等）
     */
    @Schema(description = "规则代码", example = "sign_in")
    private String ruleCode;

    /**
     * 规则名称
     */
    @Schema(description = "规则名称", example = "每日签到")
    private String ruleName;

    /**
     * 基础积分值
     */
    @Schema(description = "基础积分值", example = "10")
    private Integer basePoints;

    /**
     * 每日上限（-1 表示无限制）
     */
    @Schema(description = "每日上限", example = "100")
    private Integer dailyLimit;

    /**
     * 连续奖励配置（JSON 格式，如 {"3":5,"7":20}）
     */
    @Schema(description = "连续奖励配置", example = "{\"3\":5,\"7\":20}")
    private String streakBonus;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "每日签到奖励")
    private String remark;

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
