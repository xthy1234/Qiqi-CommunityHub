package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 反馈状态枚举
 */
@Getter
@AllArgsConstructor
public enum FeedbackStatus {
    PENDING(0, "待处理"),
    REPLIED(1, "已回复"),
    CLOSED(2, "已关闭");
    
    private final Integer code;
    private final String description;
    
    /**
     * 根据编码获取枚举
     */
    public static FeedbackStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (FeedbackStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的反馈状态编码：" + code);
    }
}