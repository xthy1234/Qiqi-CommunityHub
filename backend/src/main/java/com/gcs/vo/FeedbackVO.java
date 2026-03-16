package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 反馈基础视图对象 - 用于列表展示
 */
@Data
@Schema(description = "反馈基础视图对象")
public class FeedbackVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;
    
    @Schema(description = "反馈内容", example = "这个功能很好用，希望能继续优化")
    private String content;
    
    @Schema(description = "状态 (0:待处理，1:已回复，2:已关闭)", example = "0")
    private FeedbackStatus status;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
