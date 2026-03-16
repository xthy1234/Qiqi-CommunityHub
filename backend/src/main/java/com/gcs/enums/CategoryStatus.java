package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分类状态枚举
 */
@Getter
@AllArgsConstructor
public enum CategoryStatus {
    ENABLED(0, "启用"),
    DISABLED(1, "禁用");
    
    private final Integer code;
    private final String description;
    
    /**
     * 根据编码获取枚举
     */
    public static CategoryStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (CategoryStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的分类状态编码：" + code);
    }
}
