package com.gcs.dto;

import com.gcs.enums.AuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 帖子通用数据传输对象
 */
@Data
@Schema(description = "帖子数据传输对象")
public class ArticleDTO {
    
    @Schema(description = "主键 ID（更新时需要）", example = "1")
    private Long id;
    
    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", required = true, example = "我的第一篇帖子")
    private String title;
    
    @Schema(description = "封面图片 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;
    
    @NotNull(message = "分类 ID 不能为空")
    @Schema(description = "内容分类 ID", required = true, example = "1")
   private Long categoryId;
    
    @Schema(description = "作者 ID", example = "1")
   private Long authorId;
    
    @Schema(description = "收藏数量", example = "10")
    private Integer favoriteCount;
    
    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;

    @Schema(description = "内容详情")
    private Map<String, Object> content;

    @Schema(description = "扩展信息 (JSON格式)")
    private Map<String, Object> extra;
    
    @Schema(description = "发布时间", example = "2026-01-01")
    private String publishTime;
    
    @Schema(description = "点赞数", example = "100")
    private Integer likeCount;
    
    @Schema(description = "点踩数", example = "5")
    private Integer dislikeCount;

    @Schema(description = "分享次数", example = "15")
    private Integer shareCount;

    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿)", example = "1")
    private AuditStatus auditStatus;

    @Schema(description = "审核回复内容", example = "审核通过")
    private String auditReply;
}
