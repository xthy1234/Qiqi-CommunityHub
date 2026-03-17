package com.gcs.interceptor;

import jakarta.servlet.http.HttpServletRequest;
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
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
            HttpServletRequest servletRequest = serverRequest.getServletRequest();
            
            // 从请求参数中获取 userId（实际项目中应该验证 Token）
            String userId = servletRequest.getParameter("userId");
            String token = servletRequest.getParameter("token");
            
            // TODO: 这里应该验证 token 的有效性
            // 简单示例：检查 userId 是否存在
            if (userId != null && !userId.isEmpty()) {
                attributes.put("userId", userId);
                return true;
            } else {
                return false; // 拒绝连接
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后的处理，可以留空
    }
}
