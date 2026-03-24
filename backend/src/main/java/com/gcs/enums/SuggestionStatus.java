package com.gcs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 修改建议状态枚举
 */
@Getter
@AllArgsConstructor
public enum SuggestionStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String description;

    /**
     * Jackson 反序列化使用
     */
    @JsonCreator
    public static SuggestionStatus fromJson(Integer value) {
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
    public static SuggestionStatus valueOfCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (SuggestionStatus type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的建议状态编码：" + code);
    }

    /**
     * 根据描述获取枚举
     */
    public static SuggestionStatus fromDescription(String description) {
        if (description == null) {
            return null;
        }

        for (SuggestionStatus type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }
}
