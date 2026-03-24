package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 贡献者 VO
 */
@Data
@Schema(description = "文章贡献者视图对象")
public class ContributorVO {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户头像", example = "http://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "贡献行数", example = "50")
    private Integer contributedLines;

    @Schema(description = "首次贡献时间", example = "2026-01-01 12:00:00")
    private LocalDateTime contributionTime;
}
