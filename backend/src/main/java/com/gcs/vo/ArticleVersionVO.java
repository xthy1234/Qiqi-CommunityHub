package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章版本 VO
 */
@Data
@Schema(description = "文章版本视图对象")
public class ArticleVersionVO {

    @Schema(description = "版本 ID", example = "1")
    private Long id;

    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    @Schema(description = "版本号", example = "3")
    private Integer version;

    @Schema(description = "标题", example = "我的文章")
    private String title;

    @Schema(description = "修改摘要", example = "更新了标题")
    private String changeSummary;

    @Schema(description = "操作人 ID", example = "1")
    private Long operatorId;

    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
}
