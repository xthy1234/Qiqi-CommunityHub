package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章版本简略 VO
 * 用于版本历史列表展示
 */
@Data
@Schema(description = "文章版本简略视图对象")
public class ArticleVersionSimpleVO {

    @Schema(description = "版本 ID", example = "1")
    private Long id;

    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    @Schema(description = "版本号", example = "1")
    private Integer version;

    @Schema(description = "主版本号", example = "2")
    private Integer majorVersion;

    @Schema(description = "次版本号", example = "3")
    private Integer minorVersion;

    @Schema(description = "文章标题", example = "Java 并发编程指南")
    private String title;

    @Schema(description = "修改摘要", example = "优化了线程池示例代码")
    private String changeSummary;

    @Schema(description = "操作人 ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作人信息")
    private UserSimpleVO operator;

    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
}
