package com.gcs.interceptor;

import com.gcs.entity.Token;
import com.gcs.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 用于验证用户身份
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("=== WebSocket 握手开始 ===");
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
            HttpServletRequest servletRequest = serverRequest.getServletRequest();
            
            // 方式 1：优先尝试 Token 认证（最可靠）
            String userIdStr = servletRequest.getParameter("userId");
            String token = servletRequest.getParameter("token");
            
            if (userIdStr != null && !userIdStr.isEmpty() && 
                token != null && !token.isEmpty()) {
                log.info("🔑 使用 Token 认证，userId={}", userIdStr);
                
                try {
                    Long userId = Long.parseLong(userIdStr);
                    
                    // 验证 Token 有效性（查询数据库）
                    Token tokenEntity = tokenService.validateAndGetToken(token);
                    if (tokenEntity == null) {
                        log.warn("❌ Token 无效或已过期");
                        return false;
                    }
                    
                    // 验证用户状态
                    if (tokenEntity.getStatus() != com.gcs.enums.CommonStatus.ENABLED) {
                        log.warn("❌ 用户账号已被禁用，userId={}", userId);
                        return false;
                    }
                    
                    // 验证通过
                    attributes.put("userId", userId);
                    attributes.put("token", token);
                    attributes.put("account", tokenEntity.getAccount());
                    attributes.put("roleId", tokenEntity.getRoleId());
                    attributes.put("authType", "TOKEN");
                    
                    log.info("✅ Token 认证成功：userId={}, account={}", 
                             userId, tokenEntity.getAccount());
                    return true;
                    
                } catch (Exception e) {
                    log.error("❌ Token 认证失败", e);
                    return false;
                }
            }
            
            // 方式 2：Token 缺失时，尝试 Session 认证（兼容性）
            log.info("⚠️ 未提供 Token，尝试 Session 认证");
            var session = servletRequest.getSession(false);
            
            if (session != null) {
                Long userIdFromSession = (Long) session.getAttribute("userId");
                
                if (userIdFromSession != null) {
                    log.info("✅ Session 认证成功，userId={}", userIdFromSession);
                    
                    String accountFromSession = (String) session.getAttribute("account");
                    Long roleIdFromSession = (Long) session.getAttribute("roleId");
                    
                    attributes.put("userId", userIdFromSession);
                    attributes.put("account", accountFromSession != null ? accountFromSession : "unknown");
                    attributes.put("roleId", roleIdFromSession != null ? roleIdFromSession : 1L);
                    attributes.put("authType", "SESSION");
                    
                    log.info("✅ Session 认证成功：userId={}, account={}",
                            userIdFromSession, accountFromSession);
                    return true;
                }
            }
            
            log.warn("❌ 认证失败：既没有 Token 也没有有效的 Session");
            return false;
        }
        
        return false;
    }


    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后的处理，可以留空
    }
}
