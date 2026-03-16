package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 包含用户信息的反馈视图对象
 */
@Data
@Schema(description = "包含用户信息的反馈视图对象")
public class FeedbackWithUserVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户账号", example = "user@example.com")
    private String userAccount;
    
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;
    
    @Schema(description = "用户头像", example = "http://example.com/avatar.jpg")
    private String userAvatar;
    
    @Schema(description = "反馈内容", example = "这个功能很好用，希望能继续优化")
    private String content;
    
    @Schema(description = "回复内容", example = "感谢您的反馈，我们会继续努力改进")
    private String replyContent;
    
    @Schema(description = "回复时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;
    
    @Schema(description = "状态 (0:待处理，1:已回复，2:已关闭)", example = "0")
    private FeedbackStatus status;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
