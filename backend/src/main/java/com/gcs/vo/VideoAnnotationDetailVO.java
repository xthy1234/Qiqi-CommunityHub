package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频注释详情视图（含用户信息）
 * @author 
 * @date 2026-04-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "视频注释详情视图")
public class VideoAnnotationDetailVO extends VideoAnnotationVO {

    @Schema(description = "创建者信息")
    private UserSimpleVO creator;

    @Schema(description = "状态（1:正常 0:禁用）", example = "1")
    private Integer status;
}
