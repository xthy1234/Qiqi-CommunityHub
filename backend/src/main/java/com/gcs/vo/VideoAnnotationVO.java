package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频注释基础视图
 * @author 
 * @date 2026-04-05
 */
@Data
@Schema(description = "视频注释基础视图")
public class VideoAnnotationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "注释 ID", example = "1")
    private Long id;

    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    @Schema(description = "视频 URL", example = "/files/123")
    private String videoUrl;

    @Schema(description = "开始时间（秒）", example = "30.500")
    private BigDecimal startTime;

    @Schema(description = "结束时间（秒）", example = "45.200")
    private BigDecimal endTime;

    @Schema(description = "注释标题", example = "关键操作点")
    private String title;

    @Schema(description = "注释内容", example = "这里要快速点击...")
    private String content;

    @Schema(description = "创建者 ID", example = "1")
    private Long creatorId;

    @Schema(description = "排序字段", example = "0")
    private Integer sortOrder;

    @Schema(description = "创建时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
