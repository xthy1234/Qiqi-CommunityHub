package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 轮播图基础视图对象 - 用于列表展示
 */
@Data
@Schema(description = "轮播图基础视图对象")
public class SwiperVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "轮播图标题", example = "新年活动推广")
    private String title;
    
    @Schema(description = "图片 URL", example = "http://example.com/banner.jpg")
    private String imageUrl;
    
    @Schema(description = "跳转链接", example = "http://example.com/activity")
    private String linkUrl;
    
    @Schema(description = "排序，数字越小越靠前", example = "1")
    private Integer sort;
    
    @Schema(description = "状态（0:显示，1:隐藏）", example = "0")
    private CommonStatus status;
    
    @Schema(description = "描述", example = "2026 年新年特别活动")
    private String description;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
