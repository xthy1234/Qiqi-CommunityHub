package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 互动记录状态枚举
 */
@Getter
@AllArgsConstructor
public enum InteractionStatus {
    VALID(0, "有效"),
    INVALID(1, "无效");
    
    private final Integer code;
    private final String description;
    
    /**
     * 根据编码获取枚举
     */
    public static InteractionStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (InteractionStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的互动状态编码：" + code);
    }
}
