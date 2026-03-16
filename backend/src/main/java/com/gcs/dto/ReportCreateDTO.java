package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 举报创建请求对象
 */
@Data
@Schema(description = "举报创建请求对象")
public class ReportCreateDTO {
    
    @Schema(description = "被举报内容 ID", required = true, example = "1")
    @NotBlank(message = "被举报内容 ID 不能为空")
    private Long contentId;
    
    @Schema(description = "被举报内容标题", example = "这是一篇违规文章")
    private String contentTitle;
    
    @Schema(description = "被举报内容分类", example = "文章")
    private String contentCategory;
    
    @Schema(description = "被举报用户 ID", example = "1")
    private Long reportedUserID;
    
    @Schema(description = "被举报用户账号", example = "violated_user")
    private String reportedUserAccount;
    
    @Schema(description = "被举报用户昵称", example = "违规用户")
    private String reportedNickName;
    
    @NotBlank(message = "举报原因不能为空")
    @Schema(description = "举报原因", required = true, example = "发布虚假信息")
    private String reportReason;
}
