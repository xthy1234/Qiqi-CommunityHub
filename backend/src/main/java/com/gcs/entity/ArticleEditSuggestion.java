package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 文章修改建议实体类
 * 记录用户提交的文章修改建议（待审核）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章修改建议实体")
@TableName(value = "article_edit_suggestion", autoResultMap = true)
public class ArticleEditSuggestion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章 ID
     */
    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    /**
     * 建议者 ID
     */
    @Schema(description = "建议者 ID", example = "1")
    private Long proposerId;

    /**
     * 建议标题
     */
    @Schema(description = "建议标题", example = "优化文章结构")
    private String title;

    /**
     * 建议内容（JSONB 全量）
     */
    @Schema(description = "建议内容（JSONB 全量）")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;

    /**
     * 建议额外内容（JSONB 全量）
     */
    @Schema(description = "建议内容（JSONB 全量）")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;

    /**
     * 修改摘要
     */
    @Schema(description = "修改摘要", example = "重新组织了段落")
    private String changeSummary;

    /**
     * 状态（0-待审核，1-已通过，2-已拒绝）
     */
    @Schema(description = "状态（0-待审核，1-已通过，2-已拒绝）", example = "0")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 审核时间
     */
    @Schema(description = "审核时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    /**
     * 审核人 ID
     */
    @Schema(description = "审核人 ID", example = "1")
    private Long reviewerId;
}
