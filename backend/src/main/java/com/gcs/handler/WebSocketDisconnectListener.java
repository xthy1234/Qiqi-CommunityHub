package com.gcs.handler;

import com.gcs.service.UserOnlineStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 会话断开事件监听器
 * 当 WebSocket 会话关闭时（包括异常断开）触发，清理在线状态
 */
@Slf4j
@Component
public class WebSocketDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

    @Autowired
    private UserOnlineStatusService onlineStatusService;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        
        if (userId != null) {
            String sessionId = accessor.getSessionId();
            log.info("🔴 [Session 断开] 检测到会话关闭：userId={}, sessionId={}", userId, sessionId);
            onlineStatusService.userOffline(userId);
            log.info("✅ [Session 断开] 已清理在线状态：userId={}", userId);
        } else {
            log.warn("⚠️ [Session 断开] 检测到会话关闭但未找到 userId");
        }
    }
}
