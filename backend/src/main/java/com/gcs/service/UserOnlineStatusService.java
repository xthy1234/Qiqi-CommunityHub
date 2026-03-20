package com.gcs.service;

import com.gcs.entity.UserSessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户在线状态服务
 */
@Slf4j
@Service
public class UserOnlineStatusService {
    
    // 使用 ConcurrentHashMap 存储在线用户
    private final ConcurrentHashMap<Long, UserSessionInfo> onlineUsers = new ConcurrentHashMap<>();
    
    /**
     * 用户上线
     */
    public void userOnline(Long userId, String sessionId) {
        UserSessionInfo sessionInfo = new UserSessionInfo();
        sessionInfo.setUserId(userId);
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setOnlineTime(System.currentTimeMillis());
        sessionInfo.updateHeartbeat(); // 初始化心跳
        
        onlineUsers.put(userId, sessionInfo);
        log.info("✅ 用户上线：userId={}, sessionId={}", userId, sessionId);
    }
    
    /**
     * 用户下线
     */
    public void userOffline(Long userId) {
        UserSessionInfo removed = onlineUsers.remove(userId);
        if (removed != null) {
            log.info("🔴 用户下线：userId={}, sessionId={}", userId, removed.getSessionId());
        }
    }
    
    /**
     * 更新心跳时间
     */
    public void updateHeartbeat(Long userId) {
        UserSessionInfo sessionInfo = onlineUsers.get(userId);
        if (sessionInfo != null) {
            sessionInfo.updateHeartbeat();
        }
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }
    
    /**
     * 获取所有在线用户 ID
     */
    public Set<Long> getOnlineUserIds() {
        return onlineUsers.keySet();
    }
    
    /**
     * 获取在线用户数量
     */
    public int getOnlineCount() {
        return onlineUsers.size();
    }
    
    /**
     * 获取用户在线信息
     */
    public UserSessionInfo getUserSessionInfo(Long userId) {
        return onlineUsers.get(userId);
    }
    
    /**
     * 获取所有在线用户信息（用于定时清理）
     */
    public Map<Long, UserSessionInfo> getOnlineUsers() {
        return onlineUsers;
    }
    
    /**
     * 清理超时未心跳的用户
     * @param timeoutMillis 超时阈值（毫秒）
     * @return 清理的用户数量
     */
    public int cleanupTimeoutUsers(long timeoutMillis) {
        long now = System.currentTimeMillis();
        int cleanedCount = 0;
        
        for (Map.Entry<Long, UserSessionInfo> entry : onlineUsers.entrySet()) {
            Long userId = entry.getKey();
            UserSessionInfo info = entry.getValue();
            
            if (info.getLastHeartbeat() != null && 
                now - info.getLastHeartbeat() > timeoutMillis) {
                onlineUsers.remove(userId);
                cleanedCount++;
                log.info("⏰ 清理超时用户：userId={}, 超时时长={}ms", 
                        userId, now - info.getLastHeartbeat());
            }
        }
        
        if (cleanedCount > 0) {
            log.info("✅ 清理超时用户完成，共清理 {} 人", cleanedCount);
        }
        
        return cleanedCount;
    }
}
