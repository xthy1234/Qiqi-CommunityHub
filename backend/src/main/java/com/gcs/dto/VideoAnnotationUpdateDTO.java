package com.gcs.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 视频注释更新请求
 * @author 
 * @date 2026-04-05
 */
@Data
@Schema(description = "视频注释更新请求")
public class VideoAnnotationUpdateDTO {

    @Schema(description = "开始时间（秒）", example = "30.500")
    private BigDecimal startTime;

    @Schema(description = "结束时间（秒）", example = "45.200")
    private BigDecimal endTime;

    @Schema(description = "注释标题", example = "关键操作点")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "注释内容", example = "这里要快速点击...")
    private String content;

    @Schema(description = "排序字段", example = "0")
    private Integer sortOrder;

    @Schema(description = "状态（1:正常 0:禁用）", example = "1")
    private Integer status;
}
