package com.gcs.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频弹幕基础视图
 * @author 
 * @date 2026-04-05
 */
@Data
@Schema(description = "视频弹幕基础视图")
public class VideoDanmakuVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "弹幕 ID", example = "1")
    private Long id;

    @Schema(description = "视频 URL", example = "/files/123")
    private String videoUrl;

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "弹幕内容", example = "前方高能！")
    private String content;

    @Schema(description = "出现时间（秒）", example = "30.500")
    private BigDecimal time;

    @Schema(description = "颜色", example = "#FFFFFF")
    private String color;

    @Schema(description = "位置（0:滚动 1:顶部 2:底部）", example = "0")
    private Integer position;

    @Schema(description = "字体大小", example = "25")
    private Integer fontSize;

    @Schema(description = "是否管理员发送", example = "false")
    private Boolean isAdmin;

    @Schema(description = "创建时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
