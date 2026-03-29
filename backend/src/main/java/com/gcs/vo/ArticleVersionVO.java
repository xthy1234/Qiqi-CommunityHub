package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文章版本详细 VO
 * 用于版本详情展示和版本对比
 */
@Data
@Schema(description = "文章版本详细视图对象")
public class ArticleVersionVO {

    @Schema(description = "版本 ID", example = "1")
    private Long id;

    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    @Schema(description = "版本号", example = "1")
    private Integer version;

    @Schema(description = "文章标题", example = "Java 并发编程指南")
    private String title;

    @Schema(description = "内容详情（JSONB 全量快照）")
    private Map<String, Object> content;

    @Schema(description = "额外信息")
    private Map<String, Object> extra;

    @Schema(description = "修改摘要", example = "优化了线程池示例代码")
    private String changeSummary;

    @Schema(description = "操作人 ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作人信息")
    private UserSimpleVO operator;

    @Schema(description = "实际贡献者 ID", example = "1")
    private Long contributorId;

    @Schema(description = "实际贡献者信息")
    private UserSimpleVO contributor;

    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "是否当前最新版本", example = "true")
    private Boolean isLatest;

    @Schema(description = "版本类型（0-小版本，1-大版本）", example = "0")
    private Integer versionType;

    @Schema(description = "主版本号", example = "2")
    private Integer majorVersion;

    @Schema(description = "次版本号", example = "3")
    private Integer minorVersion;

    @Schema(description = "格式化版本号", example = "2.3")
    private String formattedVersion;

    @Schema(description = "是否为当前版本", example = "true")
    private Boolean isCurrent;
}
