package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章修改建议列表 VO
 * 用于展示建议列表，包含基本信息
 */
@Data
@Schema(description = "文章修改建议列表视图对象")
public class ArticleSuggestionSimpleVO {

    @Schema(description = "建议 ID", example = "1")
    private Long id;

    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    @Schema(description = "文章标题", example = "Java 并发编程指南")
    private String articleTitle;

    @Schema(description = "文章封面图", example = "http://example.com/cover.jpg")
    private String articleCoverUrl;

    @Schema(description = "建议者信息")
    private UserSimpleVO proposer;

    @Schema(description = "建议标题", example = "优化文章结构")
    private String title;

    @Schema(description = "建议基于的文章版本号", example = "1")
    private Integer version;

    @Schema(description = "修改摘要/说明", example = "重新组织了段落，优化了代码示例")
    private String changeSummary;

    @Schema(description = "状态（0-待审核，1-已通过，2-已拒绝）", example = "0")
    private Integer status;

    @Schema(description = "建议提交时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "审核时间", example = "2026-01-01 14:30:00")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人信息")
    private UserSimpleVO reviewer;

    @Schema(description = "应用后的版本号（审核通过后填充）", example = "2")
    private Integer appliedVersion;
}
