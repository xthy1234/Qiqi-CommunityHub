package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 私信消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageType {
    TEXT(0, "文本"),
    IMAGE(1, "图片"),
    FILE(2, "文件");
    
    private final Integer code;
    private final String description;
    
    public static MessageType valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (MessageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的消息类型编码：" + code);
    }
}
