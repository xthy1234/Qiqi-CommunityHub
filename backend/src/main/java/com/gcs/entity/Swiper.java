package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import com.gcs.enums.CommonStatus;

/**
 * 轮播图实体类
 * 用于存储轮播图配置信息
 * @author 
 * @email 
 * @date 2026-04-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "轮播图实体")
@TableName("swiper")
public class Swiper implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 轮播图标题
     */
    @Schema(description = "轮播图标题", example = "新年活动推广")
    private String title;

    /**
     * 图片 URL
     */
    @Schema(description = "图片 URL", example = "http://example.com/banner.jpg")
    private String imageUrl;

    /**
     * 跳转链接
     */
    @Schema(description = "跳转链接", example = "http://example.com/activity")
    private String linkUrl;

    /**
     * 排序，数字越小越靠前
     */
    @Schema(description = "排序，数字越小越靠前", example = "1")
    private Integer sort = 0;

    /**
     * 状态（0:启用，1:禁用）
     */
    @Schema(description = "状态 (0:启用，1:禁用)", example = "0")
    private CommonStatus status = CommonStatus.ENABLED;

    /**
     * 描述
     */
    @Schema(description = "描述", example = "2026 年新年特别活动")
    private String description;
    
    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean isDeleted = false;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
