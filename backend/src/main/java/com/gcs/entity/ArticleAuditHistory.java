package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章审核历史实体
 */
@Data
@TableName("article_audit_history")
public class ArticleAuditHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 审核员 ID
     */
    private Long reviewerId;

    /**
     * 原审核状态 (0:待审核，1:通过，2:拒绝)
     */
    private Integer oldStatus;

    /**
     * 新审核状态 (0:待审核，1:通过，2:拒绝)
     */
    private Integer newStatus;

    /**
     * 审核意见/拒绝理由
     */
    private String reason;

    /**
     * 审核时间
     */
    private LocalDateTime createTime;
}
