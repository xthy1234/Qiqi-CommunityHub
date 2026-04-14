package com.gcs.utils;

import com.gcs.entity.Token;
import com.gcs.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class SessionUtils {
    
    @Autowired(required = false)
    private TokenService tokenService;
    
    /**
     * 获取当前登录用户 ID
     * 优先从 Session 获取，如果 Session 中没有则尝试从 Token 获取
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        
        if (userIdObj != null) {
            if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                try {
                    return Long.parseLong((String) userIdObj);
                } catch (NumberFormatException e) {
                    log.warn("Session 中 userId 格式错误: {}", userIdObj);
                }
            }
        }
        
        log.debug("Session 中未找到 userId，尝试从 Token 获取");
        return getUserIdFromToken(request);
    }
    
    /**
     * 从 Token 中获取用户 ID（降级方案）
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        if (tokenService == null) {
            log.warn("TokenService 未注入，无法从 Token 获取用户 ID");
            return null;
        }
        
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        try {
            Token tokenEntity = tokenService.validateAndGetToken(token);
            if (tokenEntity != null) {
                log.debug("从 Token 获取到 userId: {}", tokenEntity.getUserId());
                return tokenEntity.getUserId();
            }
        } catch (Exception e) {
            log.warn("从 Token 获取用户 ID 失败", e);
        }
        
        return null;
    }
    
    /**
     * 从请求中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        String token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        return request.getParameter("token");
    }
    
    /**
     * 获取会话属性
     */
    public String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }
    
    /**
     * 设置会话属性
     */
    public void setSessionAttribute(HttpServletRequest request, String attributeName, Object value) {
        request.getSession().setAttribute(attributeName, value);
    }
    
    /**
     * 使会话失效（退出登录）
     */
    public void invalidateSession(HttpServletRequest request) {
        request.getSession().invalidate();
    }
}
