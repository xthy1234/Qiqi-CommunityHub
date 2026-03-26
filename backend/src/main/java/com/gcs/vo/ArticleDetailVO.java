package com.gcs.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.gcs.enums.AuditStatus;
import com.gcs.handler.AuditStatusTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 帖子详情视图对象 - 包含完整信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "帖子详情视图对象")
public class ArticleDetailVO extends ArticleVO {
    
    @Schema(description = "内容详情")
    private Map<String, Object> content;

    @Schema(description = "扩展信息 (JSON格式)")
    private Map<String, Object> extra;

    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;
    
    @Schema(description = "收藏数量", example = "10")
    private Integer favoriteCount;
    
    @Schema(description = "审核回复内容", example = "审核通过")
    private String auditReply;
    
    @Schema(description = "编辑模式（0-仅作者可编辑，1-所有人可建议）", example = "0")
    private Integer editMode;

    @Schema(description = "评论数量", example = "20")
    private Integer commentCount;

    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    private String updateTime;

    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿)", example = "1")
    private Integer auditStatus;

    @Schema(description = "分享次数", example = "15")
    private Integer shareCount = 0;
}
