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
    COMMENT(1, "文章被评论"),
    LIKE(2, "点赞"),
    FOLLOW(3, "关注"),
    REPLY(4, "评论被回复"),
    ARTICLE_AUDIT(5, "文章审核结果"),
    CIRCLE_INVITE(6, "邀请进入圈子"),
    CIRCLE_REMOVED(7, "被移出圈子"),
    CIRCLE_JOIN(8, "圈子申请加入"),
    MEMBER_JOIN(9, "圈子有新成员加入"),
    MEMBER_QUIT(10, "圈子成员退出"),
    MEMBER_REMOVED(11, "圈子成员被移出"),
    SUGGESTION_SUBMIT(12, "有人为你的文章提交修改建议"),
    SUGGESTION_REVIEW(13, "建议审核结果"),
    SYSTEM_MESSAGE(14, "系统通知");

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
