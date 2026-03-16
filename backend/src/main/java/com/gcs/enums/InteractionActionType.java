package com.gcs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 互动操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum InteractionActionType {
    FAVORITE(1, "收藏"),
    LIKE(2, "点赞"),
    DISLIKE(3, "点踩"),
    SHARE(4, "分享");

    private final Integer code;
    private final String description;

    /**
     * Jackson 反序列化使用（将整数或字符串转为枚举）
     */
    @JsonCreator
    public static InteractionActionType fromJson(Object value) {
        if (value == null) {
            return null;
        }
        
        // 如果是数字，按 code 匹配
        if (value instanceof Integer) {
            return valueOf((Integer) value);
        }
        
        // 如果是字符串，尝试按 code 或 name 匹配
        if (value instanceof String) {
            try {
                Integer code = Integer.parseInt((String) value);
                return valueOf(code);
            } catch (NumberFormatException e) {
                // 尝试按名称匹配
                for (InteractionActionType type : values()) {
                    if (type.name().equalsIgnoreCase((String) value)) {
                        return type;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Jackson 序列化使用（将枚举转为 code）
     */
    @JsonValue
    public Integer toJson() {
        return this.code;
    }

    /**
     * 根据编码获取枚举
     */
    public static InteractionActionType valueOf(Integer code) {
        if (code == null) {
            return null;
        }

        for (InteractionActionType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的互动操作类型编码：" + code);
    }

    /**
     * 根据描述获取枚举
     */
    public static InteractionActionType fromDescription(String description) {
        if (description == null) {
            return null;
        }

        for (InteractionActionType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }
}
