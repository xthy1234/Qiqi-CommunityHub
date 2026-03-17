package com.gcs.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * WebSocket 消息拦截器
 * 用于验证每个消息的发送者身份
 */
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        // 在 CONNECT 时验证用户
        if (StompCommand.CONNECT.equals(command)) {
            String userId = accessor.getFirstNativeHeader("userId");
            String token = accessor.getFirstNativeHeader("token");

            // TODO: 验证 token 有效性
            if (userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("用户未认证");
            }

            accessor.setUser(() -> userId);
        }

        return message;
    }
}
