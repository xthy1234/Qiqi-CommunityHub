package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 反馈详情视图对象 - 包含完整信息和回复
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "反馈详情视图对象")
public class FeedbackDetailVO extends FeedbackVO {
    
    @Schema(description = "回复内容", example = "感谢您的反馈，我们会继续努力改进")
    private String replyContent;
    
    @Schema(description = "回复时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @Schema(description = "处理人账号", example = "admin")
    private String handlerAccount;
}
