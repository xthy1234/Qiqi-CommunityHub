package com.gcs.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 弹幕发送请求
 * @author 
 * @date 2026-04-05
 */
@Data
@Schema(description = "弹幕发送请求")
public class DanmakuSendDTO {

    @Schema(description = "视频 URL", required = true, example = "/files/123")
    @NotBlank(message = "视频 URL 不能为空")
    private String videoUrl;

    @Schema(description = "文章 ID（可选）", example = "1")
    private Long articleId;

    @Schema(description = "弹幕内容", required = true, example = "前方高能！")
    @NotBlank(message = "弹幕内容不能为空")
    private String content;

    @Schema(description = "出现时间（秒）", required = true, example = "30.500")
    @NotNull(message = "出现时间不能为空")
    private BigDecimal time;

    @Schema(description = "颜色（十六进制）", example = "#FFFFFF")
    private String color = "#FFFFFF";

    @Schema(description = "位置（0:滚动 1:顶部 2:底部）", example = "0")
    private Integer position = 0;

    @Schema(description = "字体大小（像素）", example = "25")
    private Integer fontSize = 25;
}
