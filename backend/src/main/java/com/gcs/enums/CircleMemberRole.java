package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 圈子成员角色枚举
 */
@Getter
@AllArgsConstructor
public enum CircleMemberRole {
    MEMBER(0, "成员"),
    ADMIN(1, "管理员"),
    OWNER(2, "圈主");

    @EnumValue
    private final Integer code;
    private final String description;

    /**
     * 根据编码获取枚举
     */
    public static CircleMemberRole valueOf(Integer code) {
        if (code == null) {
            return null;
        }

        for (CircleMemberRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的成员角色编码：" + code);
    }
}
