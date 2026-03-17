package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 圈子类型枚举
 */
@Getter
@AllArgsConstructor
public enum CircleType {
    PRIVATE(0, "私密圈子"),
    PUBLIC(1, "公开圈子"),
    PERSONAL(2, "个人空间");
    
    @EnumValue
    private final Integer code;
    private final String description;
    
    /**
     * 根据编码获取枚举
     */
    public static CircleType valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (CircleType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的圈子类型编码：" + code);
    }
}
