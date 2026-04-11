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
 * 视频弹幕实体类
 * @author 
 * @date 2026-04-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频弹幕实体")
@TableName("video_danmaku")
public class VideoDanmaku implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "文章 ID（可选）", example = "1")
    private Long articleId;

    @Schema(description = "视频 URL", example = "/files/123")
    private String videoUrl;

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "弹幕内容", example = "前方高能！")
    private String content;

    @Schema(description = "出现时间（秒）", example = "30.500")
    private BigDecimal time;

    @Schema(description = "颜色（十六进制）", example = "#FFFFFF")
    private String color;

    @Schema(description = "位置（0:滚动 1:顶部 2:底部）", example = "0")
    private Integer position;

    @Schema(description = "字体大小（像素）", example = "25")
    private Integer fontSize;

    @Schema(description = "是否管理员发送", example = "false")
    private Boolean isAdmin;

    @Schema(description = "状态（1:正常 0:屏蔽）", example = "1")
    private Integer status;

    @Schema(description = "IP 地址", example = "192.168.1.1")
    private String ipAddress;

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
