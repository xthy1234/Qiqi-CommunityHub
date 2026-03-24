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
 * 文章贡献者实体类
 * 记录文章的贡献者及其贡献行数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章贡献者实体")
@TableName("article_contributor")
public class ArticleContributor implements Serializable {
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
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /**
     * 贡献行数
     */
    @Schema(description = "贡献行数", example = "50")
    private Integer contributedLines;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
