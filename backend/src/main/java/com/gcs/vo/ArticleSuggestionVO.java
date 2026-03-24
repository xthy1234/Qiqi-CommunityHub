package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文章建议 VO
 */
@Data
@Schema(description = "文章修改建议视图对象")
public class ArticleSuggestionVO {

    @Schema(description = "建议 ID", example = "1")
    private Long id;

    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    @Schema(description = "建议者 ID", example = "2")
    private Long proposerId;

    @Schema(description = "建议者昵称", example = "张三")
    private String proposerNickname;

    @Schema(description = "建议者头像", example = "http://example.com/avatar.jpg")
    private String proposerAvatar;

    @Schema(description = "建议标题", example = "优化文章结构")
    private String title;

    @Schema(description = "修改摘要", example = "重新组织了段落")
    private String changeSummary;

    @Schema(description = "状态（0-待审核，1-已通过，2-已拒绝）", example = "0")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "审核时间", example = "2026-01-01 12:00:00")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人 ID", example = "1")
    private Long reviewerId;

    @Schema(description = "新增行数", example = "15")
    private Integer addedLines;

    @Schema(description = "删除行数", example = "10")
    private Integer removedLines;
}
