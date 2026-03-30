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

/**
 * 积分流水实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "积分流水实体")
@TableName("points_transaction")
public class PointsTransaction implements Serializable {
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
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /**
     * 变动金额（正增负减）
     */
    @Schema(description = "变动金额", example = "10")
    private Integer amount;

    /**
     * 变动后余额
     */
    @Schema(description = "变动后余额", example = "100")
    private Integer balance;

    /**
     * 来源（sign_in, post_article, like_received 等）
     */
    @Schema(description = "来源", example = "sign_in")
    private String source;

    /**
     * 关联业务 ID（如文章 ID、评论 ID 等）
     */
    @Schema(description = "关联业务 ID", example = "123")
    private Long sourceId;

    /**
     * 描述
     */
    @Schema(description = "描述", example = "每日签到")
    private String description;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
