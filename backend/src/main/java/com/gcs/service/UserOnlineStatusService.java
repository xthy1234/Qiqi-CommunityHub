package com.gcs.service;

import com.gcs.dao.UserDao;
import com.gcs.entity.UserSessionInfo;
import com.gcs.vo.OnlineStatusVO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户在线状态服务
 */
@Slf4j
@Service
public class UserOnlineStatusService {
    
    @Autowired
    private UserDao userDao;
    
    @Lazy
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
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
        sessionInfo.updateHeartbeat();
        
        onlineUsers.put(userId, sessionInfo);
        
        log.info(" 用户上线：userId={}, sessionId={}", userId, sessionId);
        
        try {
            notifyOnlineStatusChange(userId, true);
        } catch (Exception e) {
            log.error("推送上线状态失败：userId={}", userId, e);
        }
    }
    
    /**
     * 用户下线
     */
    public void userOffline(Long userId) {
        UserSessionInfo removed = onlineUsers.remove(userId);
        if (removed != null) {
            log.info(" 用户下线：userId={}, sessionId={}", userId, removed.getSessionId());
            
            try {
                userDao.updateLastOnlineTime(userId);
                log.debug(" 已更新数据库最后离线时间：userId={}", userId);
            } catch (Exception e) {
                log.error("更新最后离线时间失败：userId={}", userId, e);
            }
            
            try {
                notifyOnlineStatusChange(userId, false);
            } catch (Exception e) {
                log.error("推送离线状态失败：userId={}", userId, e);
            }
        } else {
            log.debug("用户 {} 不在在线列表中，可能已经下线", userId);
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
     * 获取用户的最后活跃时间戳
     * @param userId 用户 ID
     * @return 时间戳（毫秒），如果用户从未上线则返回 null
     */
    public Long getLastSeenAt(Long userId) {
        if (isOnline(userId)) {
            UserSessionInfo sessionInfo = onlineUsers.get(userId);
            return sessionInfo != null ? sessionInfo.getLastHeartbeat() : System.currentTimeMillis();
        } else {
            java.time.LocalDateTime lastOnlineTime = userDao.selectLastOnlineTime(userId);
            if (lastOnlineTime != null) {
                return lastOnlineTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            return null;
        }
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
                
                try {
                    userDao.updateLastOnlineTime(userId);
                } catch (Exception e) {
                    log.error("清理前更新时间失败：userId={}", userId, e);
                }
                
                onlineUsers.remove(userId);
                cleanedCount++;
                log.info(" 清理超时用户：userId={}, 超时时长={}ms",
                        userId, now - info.getLastHeartbeat());
            }
        }
        
        if (cleanedCount > 0) {
            log.info(" 清理超时用户完成，共清理 {} 人", cleanedCount);
        }
        
        return cleanedCount;
    }
    
    /**
     * 向与指定用户有会话的人推送在线状态变更
     * @param userId 状态变更的用户 ID
     * @param isOnline 是否在线
     */
    private void notifyOnlineStatusChange(Long userId, boolean isOnline) {
        try {
            List<Long> relatedUserIds = findRelatedUserIds(userId);
            
            if (relatedUserIds.isEmpty()) {
                log.debug("用户 {} 没有会话记录，跳过推送", userId);
                return;
            }
            
            OnlineStatusVO statusVO = new OnlineStatusVO();
            statusVO.setUserId(userId);
            statusVO.setIsOnline(isOnline);
            statusVO.setTimestamp(System.currentTimeMillis());
            statusVO.setLastSeenAt(getLastSeenAt(userId));
            
            for (Long relatedUserId : relatedUserIds) {
                if (!userId.equals(relatedUserId)) {
                    String destination = "/user/" + relatedUserId + "/queue/online-status-changes";
                    log.debug("推送在线状态变更：用户 {} -> 用户 {} ({})",
                             userId, relatedUserId, isOnline ? "上线" : "下线");
                    
                    try {
                        messagingTemplate.convertAndSend(destination, statusVO);
                    } catch (Exception e) {
                        log.error("推送失败：destination={}", destination, e);
                    }
                }
            }
            
            log.info(" 已推送在线状态变更：userId={}, isOnline={}, 接收人数={}", 
                    userId, isOnline, relatedUserIds.size());
                    
        } catch (Exception e) {
            log.error("推送在线状态变更失败：userId={}, isOnline={}", userId, isOnline, e);
        }
    }
    
    /**
     * 查找与指定用户有会话的所有用户 ID
     * @param userId 用户 ID
     * @return 相关用户 ID 列表
     */
    private List<Long> findRelatedUserIds(Long userId) {
        Set<Long> relatedUserSet = new HashSet<>();
        
        try {
            // 从私信表中查找最近联系人（通过 from_user_id 或 to_user_id）
            List<Long> messageUserIds = userDao.selectRelatedUserIds(userId);
            if (messageUserIds != null && !messageUserIds.isEmpty()) {
                relatedUserSet.addAll(messageUserIds);
                log.debug(" 从私信表找到 {} 个相关用户", messageUserIds.size());
            }
        } catch (Exception e) {
            log.error("查找相关用户失败：userId={}", userId, e);
        }
        
        return new ArrayList<>(relatedUserSet);
    }
}
