package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户签到记录实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户签到记录实体")
@TableName("user_sign_in")
public class UserSignIn implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "123")
    private Long userId;

    /**
     * 签到日期
     */
    @Schema(description = "签到日期", example = "2026-01-01")
    private LocalDate signDate;

    /**
     * 获得积分
     */
    @Schema(description = "获得积分", example = "10")
    private Integer pointsEarned;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
