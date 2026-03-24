package com.gcs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章编辑模式枚举
 */
@Getter
@AllArgsConstructor
public enum EditMode {
    AUTHOR_ONLY(0, "仅作者可编辑"),
    ALL_SUGGEST(1, "所有人可建议");

    private final Integer code;
    private final String description;

    /**
     * Jackson 反序列化使用
     */
    @JsonCreator
    public static EditMode fromJson(Integer value) {
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
    public static EditMode valueOfCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (EditMode type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的编辑模式编码：" + code);
    }

    /**
     * 根据描述获取枚举
     */
    public static EditMode fromDescription(String description) {
        if (description == null) {
            return null;
        }

        for (EditMode type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }
}
