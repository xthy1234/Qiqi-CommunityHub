package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 圈子成员状态枚举
 */
@Getter
@AllArgsConstructor
public enum MemberStatus {
    PENDING(0, "待审核"),
    ACTIVE(1, "正常"),
    INACTIVE(2, "已退出/被移除");
    
    private final Integer code;
    private final String description;
    
    public static MemberStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (MemberStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的成员状态编码：" + code);
    }
}
