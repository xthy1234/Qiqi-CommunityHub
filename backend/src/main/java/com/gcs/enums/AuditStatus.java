package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审核状态枚举
 */
@Getter
@AllArgsConstructor
public enum AuditStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String description;
    
    /**
     * 根据编码获取枚举
     */
    public static AuditStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (AuditStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的审核状态编码：" + code);
    }
}




