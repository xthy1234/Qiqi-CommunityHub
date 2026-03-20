package com.gcs.entity;

import lombok.Data;

/**
 * 用户会话信息
 */
@Data
public class UserSessionInfo {
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 上线时间戳
     */
    private Long onlineTime;
    
    /**
     * 最后心跳时间
     */
    private Long lastHeartbeat;
    
    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
