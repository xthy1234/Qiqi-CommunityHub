package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文章贡献者 VO
 */
@Data
@Schema(description = "文章贡献者视图对象")
public class ArticleContributorVO {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户头像", example = "http://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "累计新增行数", example = "100")
    private Integer addedLines;

    @Schema(description = "累计修改行数", example = "50")
    private Integer modifiedLines;

    @Schema(description = "累计删除行数", example = "20")
    private Integer deletedLines;

    @Schema(description = "贡献分数", example = "125.00")
    private BigDecimal score;

    @Schema(description = "最后贡献时间", example = "2026-01-01 12:00:00")
    private LocalDateTime lastContributedAt;
}
