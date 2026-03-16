package com.gcs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论状态枚举
 */
@Getter
@AllArgsConstructor
public enum CommentStatus {
    SHOW(0, "显示"),
    HIDE(1, "隐藏");
    
    private final Integer code;
    private final String description;
    
    /**
     * 根据编码获取枚举
     */
    public static CommentStatus valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (CommentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的评论状态编码：" + code);
    }
}
