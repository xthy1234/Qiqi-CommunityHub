package com.gcs.vo;

import com.gcs.enums.AuditStatus;
import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报信息基础视图对象 - 用于列表展示
 */
@Data
@Schema(description = "举报信息基础视图对象")
public class ReportVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "被举报内容 ID", example = "100")
    private Long contentId;
    
    @Schema(description = "被举报内容标题", example = "这是一篇文章的标题")
    private String contentTitle;
    
    @Schema(description = "被举报用户 ID", example = "50")
    private Long reportedUserId;
    
    @Schema(description = "被举报用户信息")
    private UserSimpleVO reportedUserInfo;
    
    @Schema(description = "举报原因", example = "发布违规信息")
    private String reportReason;
    
    @Schema(description = "举报人信息")
    private UserSimpleVO reporterUserInfo;

    @Schema(description = "审核人信息")
    private UserSimpleVO reviewUserInfo;

    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)", example = "0")
    private Integer reviewStatus;
    
    @Schema(description = "状态 (0:有效，1:无效)", example = "0")
    private CommonStatus status;
    
    @Schema(description = "举报时间", example = "2026-01-01 12:00:00")
    private LocalDateTime reportTime;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
}
