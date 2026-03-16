package com.gcs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 互动内容类型枚举
 */
@Getter
@AllArgsConstructor
public enum ContentType {
    ARTICLE("article", "文章"),
    COMMENT("comment", "评论");

    private final String code;
    private final String description;

    /**
     * Jackson 反序列化使用
     */
    @JsonCreator
    public static ContentType fromJson(String value) {
        if (value == null) {
            return null;
        }
        return valueOfCode(value);
    }

    /**
     * Jackson 序列化使用
     */
    @JsonValue
    public String toJson() {
        return this.code;
    }

    /**
     * 根据编码获取枚举
     */
    public static ContentType valueOfCode(String code) {
        if (code == null) {
            return null;
        }

        for (ContentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的内容类型编码：" + code);
    }

    /**
     * 根据描述获取枚举
     */
    public static ContentType fromDescription(String description) {
        if (description == null) {
            return null;
        }

        for (ContentType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据名称获取枚举（支持大小写不敏感）
     * @param name 名称（如 "article" 或 "ARTICLE"）
     * @return 对应的枚举值，找不到则返回 null
     */
    public static ContentType fromName(String name) {
        if (name == null) {
            return null;
        }
        
        // 先尝试精确匹配（包括大写枚举名）
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 忽略，继续尝试按 code 匹配
        }
        
        // 再尝试按 code 匹配（小写）
        for (ContentType type : values()) {
            if (type.getCode().equalsIgnoreCase(name)) {
                return type;
            }
        }
        
        return null;
    }
}
