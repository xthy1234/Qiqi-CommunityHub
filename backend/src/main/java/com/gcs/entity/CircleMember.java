package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.CommentStatus;
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
 * 圈子成员实体类
 * @author
 * @email
 * @date 2026-03-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "圈子成员实体")
@TableName("circle_member")
public class CircleMember implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 圈子 ID
     */
    @Schema(description = "圈子 ID", example = "1")
    private Long circleId;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /**
     * 成员角色 (0:成员，1:管理员，2:圈主)
     */
    @Schema(description = "成员角色 (0:成员，1:管理员，2:圈主)", example = "0")
    private Integer role;

    /**
     * 加入时间
     */
    @Schema(description = "加入时间", example = "2026-03-17 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;

    /**
     * 状态 (0:正常，1:已退出/被移除)
     */
    @Schema(description = "状态 (0:正常，1:已退出/被移除)", example = "0")
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
