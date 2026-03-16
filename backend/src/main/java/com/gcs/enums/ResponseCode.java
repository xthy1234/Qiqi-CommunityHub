package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResponseCode {
    SUCCESS(0, "成功"),
    ERROR(500, "服务器错误"),
    BAD_REQUEST(400, "参数错误"),
    NOT_FOUND(404, "未找到");
    
    private final Integer code;
    private final String message;
    
    /**
     * 根据编码获取枚举
     */
    public static ResponseCode valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (ResponseCode rc : values()) {
            if (rc.getCode().equals(code)) {
                return rc;
            }
        }
        throw new IllegalArgumentException("未知的响应码编码：" + code);
    }
}
