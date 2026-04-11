package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 视频注释创建请求
 * @author 
 * @date 2026-04-05
 */
@Data
@Schema(description = "视频注释创建请求")
public class VideoAnnotationCreateDTO {

    @Schema(description = "视频 URL", required = true, example = "/files/123")
    @NotBlank(message = "视频 URL 不能为空")
    private String videoUrl;

    @Schema(description = "开始时间（秒）", required = true, example = "30.500")
    @NotNull(message = "开始时间不能为空")
    private BigDecimal startTime;

    @Schema(description = "结束时间（秒，可选）", example = "45.200")
    private BigDecimal endTime;

    @Schema(description = "注释标题", required = true, example = "关键操作点")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "注释内容", example = "这里要快速点击...")
    private String content;

    @Schema(description = "排序字段", example = "0")
    private Integer sortOrder = 0;
}
