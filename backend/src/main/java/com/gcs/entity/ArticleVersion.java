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
 * 文章版本实体类
 * 记录文章每次修改的历史版本（全量快照）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章版本实体")
@TableName(value = "article_version", autoResultMap = true)
public class ArticleVersion implements Serializable {
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
     * 版本号（从 1 开始递增）
     */
    @Schema(description = "版本号", example = "1")
    private Integer version;

    /**
     * 标题
     */
    @Schema(description = "标题", example = "我的第一篇帖子")
    private String title;

    /**
     * 内容详情（JSONB 全量快照）
     */
    @Schema(description = "内容详情（JSONB 全量快照）")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;

    /**
     * 修改摘要
     */
    @Schema(description = "修改摘要", example = "更新了标题")
    private String changeSummary;

    /**
     * 操作人 ID
     */
    @Schema(description = "操作人 ID", example = "1")
    private Long operatorId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean isDeleted = false;
}
