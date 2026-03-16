package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论详情视图对象 - 包含完整信息和回复
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "评论详情视图对象")
public class CommentDetailVO extends CommentVO {
    
    @Schema(description = "回复内容", example = "这是回复内容")
    private String replyContent;
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @Schema(description = "子评论列表（回复）")
    private List<CommentDetailVO> replies;
    
    @Schema(description = "是否已点赞", example = "false")
    private Boolean isLiked = false;
}
