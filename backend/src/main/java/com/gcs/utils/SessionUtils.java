package com.gcs.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class SessionUtils {
    
    /**
     * 获取当前登录用户 ID
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = getSessionAttribute(request, "userId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
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
