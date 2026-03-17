package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 通用状态枚举
 */
@Getter
@AllArgsConstructor
public enum CommonStatus {
    ENABLED(0, "启用/有效"),
    DISABLED(1, "禁用/无效");
    
    @EnumValue
    private final Integer code;
    private final String description;
    
    /**
     * 根据编码获取枚举
     */
    public static CommonStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (CommonStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的通用状态编码：" + code);
    }
}