package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频弹幕详情视图（含用户信息）
 * @author 
 * @date 2026-04-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "视频弹幕详情视图")
public class VideoDanmakuDetailVO extends VideoDanmakuVO {

    @Schema(description = "发送者信息")
    private UserSimpleVO sender;
}
