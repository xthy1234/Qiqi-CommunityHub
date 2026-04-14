package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 举报审核处理请求对象（含处理动作）
 */
@Data
@Schema(description = "举报审核处理请求对象")
public class ReportReviewActionDTO {
    
    @Schema(description = "举报 ID（从 URL 路径获取，无需传递）", example = "1", hidden = true)
    private Long reportId;
    
    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)", required = true, example = "1")
    private Integer reviewStatus;
    
    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容/处理备注", required = true, example = "经查证，内容确实违规")
    private String replyContent;
    
    @Schema(description = "处理动作（BLOCK:屏蔽, DELETE:删除, WARN:警告, IGNORE:忽略）", example = "BLOCK")
    private String action; // BLOCK, DELETE, WARN, IGNORE
    
    @Schema(description = "是否奖励举报人积分", example = "true")
    private Boolean rewardReporter = true;
    
    @Schema(description = "是否扣除被举报人积分", example = "true")
    private Boolean penalizeReportedUser = true;
}
