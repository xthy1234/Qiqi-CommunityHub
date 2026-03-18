package com.gcs.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StompOutboundInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        // 记录所有出站命令（不仅仅是 MESSAGE）
        log.info("🚦 [出站] 命令类型：{}", command);

        if (command != null) {
            switch (command) {
                case MESSAGE -> {
                    // 👈 这是最重要的业务消息
                    String destination = accessor.getDestination();
                    String subscriptionId = accessor.getSubscriptionId();
                    String sessionId = accessor.getSessionId();

                    log.info("📤 [出站拦截器] 正在发送 STOMP MESSAGE:");
                    log.info("   目的地：{}", destination);
                    log.info("   订阅 ID: {}", subscriptionId);
                    log.info("   Session ID: {}", sessionId);

                    try {
                        byte[] payload = (byte[]) message.getPayload();
                        String content = new String(payload);
                        log.info("   消息内容：{}", content);
                    } catch (Exception e) {
                        log.warn("无法解析消息内容：{}", e.getMessage());
                    }
                }

                case CONNECTED -> {
                    // 连接成功的响应
                    log.debug("📤 [出站] CONNECTED - 连接已建立");
                }

                case ERROR -> {
                    // 错误消息（需要特别关注）
                    log.error("📤 [出站] ERROR - 发生错误");
                    log.error("   错误消息：{}", accessor.getFirstNativeHeader("message"));
                }

                case RECEIPT -> {
                    // 对 SEND 命令的确认
                    log.debug("📤 [出站] RECEIPT - 发送确认");
                }

                default -> {
                    log.debug("📤 [出站] 未知命令类型：{}", command);
                }
            }
        }

        return message;
    }

}
