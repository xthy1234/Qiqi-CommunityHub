package com.gcs.vo;

import com.gcs.enums.AuditStatus;
import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 举报信息详情视图对象 - 包含完整信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "举报信息详情视图对象")
public class ReportDetailVO extends ReportVO {
    
    @Schema(description = "被举报用户 ID", example = "50")
    private Long reportedUserID;
    
    @Schema(description = "被举报用户账号", example = "violator_user")
    private String reportedUserAccount;
    
    @Schema(description = "回复内容", example = "已处理，感谢举报")
    private String replyContent;
    
    @Schema(description = "审核人账号", example = "admin")
    private String reviewerAccount;
    
    @Schema(description = "审核时间", example = "2026-01-01 14:00:00")
    private LocalDateTime reviewTime;
    
    @Schema(description = "更新时间", example = "2026-01-01 14:00:00")
    private LocalDateTime updateTime;
}
