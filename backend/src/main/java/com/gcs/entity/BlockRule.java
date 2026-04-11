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
 * 用户屏蔽规则实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户屏蔽规则实体")
@TableName("block_rule")
public class BlockRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "规则类型 (keyword, category, author)")
    private String ruleType;

    @Schema(description = "规则值 (关键词/分类ID/作者ID)")
    private String ruleValue;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
