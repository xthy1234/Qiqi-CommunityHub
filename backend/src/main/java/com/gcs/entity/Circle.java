package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.CommonStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 圈子实体类
 * @author 
 * @email 
 * @date 2026-03-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "圈子实体")
@TableName("circle")
public class Circle implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 圈子名称
     */
    @Schema(description = "圈子名称", example = "游戏战队")
    private String name;

    /**
     * 圈子描述
     */
    @Schema(description = "圈子描述", example = "欢迎加入我们的游戏战队")
    private String description;

    /**
     * 圈子头像 URL
     */
    @Schema(description = "圈子头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;

    /**
     * 圈主 ID
     */
    @Schema(description = "圈主 ID", example = "1")
    private Long ownerId;

    /**
     * 圈子类型 (0:私密圈子，1:公开圈子，2:个人空间)
     */
    @Schema(description = "圈子类型 (0:私密圈子，1:公开圈子，2:个人空间)", example = "1")
    private Integer type;

    /**
     * 圈子状态 (0:正常，1:解散)
     */
    @Schema(description = "圈子状态 (0:正常，1:解散)", example = "0")
    private CommonStatus status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-03-17 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-03-17 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
