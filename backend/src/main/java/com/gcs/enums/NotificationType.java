package com.gcs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationType {
    REPLY(1, "回复"),
    LIKE(2, "点赞"),
    FOLLOW(3, "关注"),
    PRIVATE_MESSAGE(4, "私聊消息"),
    CIRCLE_UPDATE(5, "圈子更新"),
    CIRCLE_CHAT_AT(6, "圈子聊天@"),
    SUGGESTION_SUBMIT(7, "提交建议"),
    SUGGESTION_REVIEW(8, "建议审核结果");

    private final Integer code;
    private final String description;

    /**
     * Jackson 反序列化使用
     */
    @JsonCreator
    public static NotificationType fromJson(Integer value) {
        if (value == null) {
            return null;
        }
        return valueOfCode(value);
    }

    /**
     * Jackson 序列化使用
     */
    @JsonValue
    public Integer toJson() {
        return this.code;
    }

    /**
     * 根据编码获取枚举
     */
    public static NotificationType valueOfCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (NotificationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的通知类型编码：" + code);
    }

    /**
     * 根据描述获取枚举
     */
    public static NotificationType fromDescription(String description) {
        if (description == null) {
            return null;
        }

        for (NotificationType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }
}
