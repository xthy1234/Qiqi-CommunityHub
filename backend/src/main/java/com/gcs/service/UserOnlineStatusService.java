package com.gcs.service;

import com.gcs.entity.UserSessionInfo;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户在线状态服务
 */
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
        
        onlineUsers.put(userId, sessionInfo);
    }
    
    /**
     * 用户下线
     */
    public void userOffline(Long userId) {
        onlineUsers.remove(userId);
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
}
