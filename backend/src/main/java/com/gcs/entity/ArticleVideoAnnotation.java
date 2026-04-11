package com.gcs.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文章视频注释实体类
 * @author 
 * @date 2026-04-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章视频注释实体")
@TableName("article_video_annotation")
public class ArticleVideoAnnotation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    @Schema(description = "视频 URL（文件ID或相对路径）", example = "/files/123")
    private String videoUrl;

    @Schema(description = "开始时间（秒）", example = "30.500")
    private BigDecimal startTime;

    @Schema(description = "结束时间（秒，可选）", example = "45.200")
    private BigDecimal endTime;

    @Schema(description = "注释标题", example = "关键操作点")
    private String title;

    @Schema(description = "注释内容（支持Markdown）", example = "这里要快速点击...")
    private String content;

    @Schema(description = "创建者 ID", example = "1")
    private Long creatorId;

    @Schema(description = "状态（1:正常 0:禁用）", example = "1")
    private Integer status;

    @Schema(description = "排序字段", example = "0")
    private Integer sortOrder;

    @Schema(description = "逻辑删除", example = "false")
    @TableLogic
    private Boolean isDeleted;

    @Schema(description = "创建时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
