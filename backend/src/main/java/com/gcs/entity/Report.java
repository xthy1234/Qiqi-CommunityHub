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

import com.gcs.enums.AuditStatus;
import com.gcs.enums.CommonStatus;

/**
 * 举报信息实体类
 * 数据库通用操作实体类（普通增删改查）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "举报信息实体")
@TableName("report")
public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被举报内容 ID
     */
    @Schema(description = "被举报内容 ID", example = "1")
    private Long contentId;

    /**
     * 被举报内容标题
     */
    @Schema(description = "被举报内容标题", example = "这是一篇违规文章")
    private String contentTitle;

    /**
     * 被举报内容分类
     */
    @Schema(description = "被举报内容分类", example = "文章")
    private String contentCategory;

    /**
     * 被举报用户账号
     */
    @Schema(description = "被举报用户 ID", example = "1")
    private Long reportedUserID;

    @Schema(description = "被举报用户账号", example = "violated_user")
    private String reportedUserAccount;

    /**
     * 被举报用户姓名
     */
    @Schema(description = "被举报用户昵称", example = "违规用户")
    private String reportedNickName;

    /**
     * 举报原因
     */
    @Schema(description = "举报原因", example = "发布虚假信息")
    private String reportReason;

    /**
     * 举报时间
     */
    @Schema(description = "举报时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;

    /**
     * 举报人账号
     */
    @Schema(description = "举报人账号", example = "reporter_user")
    private String reporterAccount;

    /**
     * 举报人 ID
     */
    @Schema(description = "举报人 ID", example = "2")
    private Long reporterId;

    /**
     * 回复内容
     */
    @Schema(description = "回复内容", example = "已处理，感谢举报")
    private String replyContent;

    /**
     * 审核状态 (0:待审核，1:已通过，2:已拒绝)
     */
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)", example = "0")
    private AuditStatus reviewStatus = AuditStatus.PENDING;

    /**
     * 审核人账号
     */
    @Schema(description = "审核人账号", example = "admin")
    private String reviewerAccount;

    /**
     * 审核时间
     */
    @Schema(description = "审核时间", example = "2026-01-01 14:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    /**
     * 状态 (0:有效，1:无效)
     */
    @Schema(description = "状态 (0:有效，1:无效)", example = "0")
    private CommonStatus status = CommonStatus.ENABLED;

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
