package com.gcs.dto;

import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播图创建请求对象
 */
@Data
@Schema(description = "轮播图创建请求对象")
public class SwiperCreateDTO {
    
    @NotBlank(message = "标题不能为空")
    @Schema(description = "轮播图标题", required = true, example = "新年活动推广")
    private String title;
    
    @NotBlank(message = "图片 URL 不能为空")
    @Schema(description = "图片 URL", required = true, example = "http://example.com/banner.jpg")
    private String imageUrl;
    
    @Schema(description = "跳转链接", example = "http://example.com/activity")
    private String linkUrl;
    
    @NotNull(message = "排序字段不能为空")
    @Schema(description = "排序，数字越小越靠前", required = true, example = "1")
    private Integer sort;
    
    @Schema(description = "状态（0:显示，1:隐藏）", example = "0")
    private CommonStatus status;
    
    @Schema(description = "描述", example = "2026 年新年特别活动")
    private String description;
}
