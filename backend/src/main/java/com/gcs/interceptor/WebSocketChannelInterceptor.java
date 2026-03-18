package com.gcs.interceptor;

import com.gcs.entity.Token;
import com.gcs.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        log.info("🔵 [拦截器] 收到 STOMP 命令：{}", command);
        log.info("🔵 [拦截器] 消息目的地：{}", accessor.getDestination());
        log.info("🔵 [拦截器] Session Attributes: {}", accessor.getSessionAttributes());

        // 在 CONNECT 时验证用户
        if (StompCommand.CONNECT.equals(command)) {
            String userIdStr = accessor.getFirstNativeHeader("userId");
            String token = accessor.getFirstNativeHeader("token");
            
            log.debug("🔵 [CONNECT] userId: {}", userIdStr);
            log.debug("🔵 [CONNECT] token: {}", token);

            // 验证参数完整性
            if (userIdStr == null || userIdStr.isEmpty()) {
                log.warn("CONNECT 消息验证失败：缺少 userId 参数");
                throw new IllegalArgumentException("用户 ID 不能为空");
            }

            if (token == null || token.isEmpty()) {
                log.warn("CONNECT 消息验证失败：缺少 token 参数");
                throw new IllegalArgumentException("认证令牌不能为空");
            }

            try {
                // 验证 Token 有效性（查询数据库）
                Token tokenEntity = tokenService.validateAndGetToken(token);
                if (tokenEntity == null) {
                    log.warn("CONNECT 消息验证失败：Token 无效或已过期，userId={}", userIdStr);
                    throw new IllegalArgumentException("认证令牌无效或已过期");
                }

                // 验证 userId 是否匹配
                Long userId = Long.parseLong(userIdStr);
                if (!tokenEntity.getUserId().equals(userId)) {
                    log.warn("CONNECT 消息验证失败：userId 与 Token 不匹配，userId={}, tokenUserId={}", 
                             userId, tokenEntity.getUserId());
                    throw new IllegalArgumentException("用户身份验证失败");
                }

                // 验证用户状态
                if (tokenEntity.getStatus() != com.gcs.enums.CommonStatus.ENABLED) {
                    log.warn("CONNECT 消息验证失败：用户账号已被禁用，userId={}", userId);
                    throw new IllegalArgumentException("用户账号已被禁用");
                }

                // 设置用户上下文（用于后续消息处理）
                accessor.setUser(() -> userIdStr);
                
                log.debug("CONNECT 消息验证成功：userId={}", userId);

            } catch (NumberFormatException e) {
                log.error("CONNECT 消息验证失败：userId 格式错误，userId={}", userIdStr, e);
                throw new IllegalArgumentException("用户 ID 格式错误");
            }
        }

        // 在其他命令时也进行验证（SUBSCRIBE、SEND 等）
        if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            // 👇 修改：从 Session Attributes 获取 userId
            Object userIdObj = accessor.getSessionAttributes().get("userId");
            String userId = userIdObj != null ? userIdObj.toString() : null;
            
            log.debug("🔵 [{}] 从 Session 获取 userId: {}", command, userId);
            
            if (userId == null) {
                log.warn("{} 消息被拒绝：用户未认证", command);
                log.warn("   Session Attributes: {}", accessor.getSessionAttributes());
                throw new IllegalArgumentException("用户未认证");
            }
            
            // 可选：如果 getUser() 为 null，同步设置一下
            if (accessor.getUser() == null) {
                accessor.setUser(() -> userId);
                log.debug("🔵 [{}] 已同步设置 User: {}", command, userId);
            }
        }

        return message;
    }
}
