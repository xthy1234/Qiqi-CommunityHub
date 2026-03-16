package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 私信消息状态枚举
 */
@Getter
@AllArgsConstructor
public enum MessageStatus {
    UNREAD(0, "未读"),
    READ(1, "已读");
    
    private final Integer code;
    private final String description;
    
    public static MessageStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (MessageStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的消息状态编码：" + code);
    }
}
