package com.gcs.config;

import com.gcs.interceptor.StompOutboundInterceptor;
import com.gcs.interceptor.WebSocketChannelInterceptor;
import com.gcs.interceptor.WebSocketHandshakeInterceptor;
import com.gcs.service.TokenService;
import com.gcs.service.UserOnlineStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类
 * 启用 STOMP 协议，定义端点与消息代理
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    @Autowired
    private WebSocketChannelInterceptor channelInterceptor;

    @Autowired
    private UserOnlineStatusService onlineStatusService;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private StompOutboundInterceptor stompOutboundInterceptor;  

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 设置客户端订阅消息的前缀（用于接收服务端推送）
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        // 设置客户端发送消息的前缀（即方法上的 @MessageMapping 会匹配此前缀）
        registry.setApplicationDestinationPrefixes("/app");
        // 设置点对点消息前缀（默认是 /user）
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 WebSocket 端点，客户端通过此端点连接
        registry.addEndpoint("/ws")
                .addInterceptors(handshakeInterceptor)  // 添加握手拦截器
                .setAllowedOriginPatterns("*");   // 生产环境应限制具体域名
//                .withSockJS();                    // 启用 SockJS 降级方案
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptor);
    }
    
    // 👇 添加这个方法
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompOutboundInterceptor);
    }
}
