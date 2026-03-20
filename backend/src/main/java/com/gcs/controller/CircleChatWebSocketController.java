package com.gcs.controller;

import com.gcs.vo.CircleChatMessage;
import com.gcs.dto.DeleteMessageRequest;
import com.gcs.dto.RecallMessageRequest;
import com.gcs.entity.CircleChat;
import com.gcs.service.CircleChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 圈子聊天 WebSocket 控制器
 * 处理实时消息发送和撤回
 */
@Slf4j
@Controller
public class CircleChatWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CircleChatService circleChatService;

    /**
     * 构造函数日志
     */
    public CircleChatWebSocketController() {
        log.info("=== CircleChatWebSocketController 已初始化 ===");
    }

    /**
     * 处理圈子消息发送
     */
    @MessageMapping("/circle-message")
    public void sendCircleMessage(@Payload CircleChatMessage chatMessage) {
        log.info("🚀 [圈子消息] 收到 WebSocket 消息：circleId={}, senderId={}", 
                 chatMessage.getCircleId(), chatMessage.getSenderId());

        try {
            // 1. 调用 Service 发送消息
            CircleChatMessage messageVO = circleChatService.sendMessage(
                chatMessage.getCircleId(),
                chatMessage.getSenderId(),
                chatMessage.getContent(),
                chatMessage.getMsgType()
            );

            log.info("✅ [圈子消息] 消息已发送：messageId={}", messageVO.getId());

            // 2. 广播给圈子所有成员
            String destination = "/topic/circles/" + chatMessage.getCircleId() + "/messages";
            log.info("📤 [圈子消息] 推送到主题：{}", destination);

            messagingTemplate.convertAndSend(destination, messageVO);

            log.info("✅ [圈子消息] 推送完成");

        } catch (Exception e) {
            log.error("❌ [圈子消息] 发送失败", e);
            log.error("   错误类型：{}", e.getClass().getName());
            log.error("   错误消息：{}", e.getMessage());
        }
    }

    /**
     * 处理撤回消息
     */
    @MessageMapping("/circle-recall-message")
    public void recallCircleMessage(@Payload RecallMessageRequest recallRequest) {
        try {
            log.info("收到撤回请求：messageId={}, userId={}", 
                    recallRequest.getMessageId(), recallRequest.getUserId());

            // 1. 执行撤回操作
            boolean success = circleChatService.recallMessage(
                recallRequest.getMessageId(), 
                recallRequest.getUserId()
            );

            if (!success) {
                log.warn("撤回失败：messageId={}, userId={}", 
                        recallRequest.getMessageId(), recallRequest.getUserId());
                return;
            }

            // 2. 查询撤回后的消息信息
            CircleChat message = circleChatService.getById(recallRequest.getMessageId());
            if (message == null) {
                return;
            }

            // 3. 组装撤回通知消息
            CircleChatMessage recallNotification = new CircleChatMessage();
            recallNotification.setId(message.getId());
            recallNotification.setCircleId(message.getCircleId());
            recallNotification.setSenderId(recallRequest.getUserId());
            recallNotification.setAction("RECALL");
            recallNotification.setContent(""); // 撤回后内容为空

            // 4. 广播给圈子所有成员
            String destination = "/topic/circles/" + message.getCircleId() + "/messages";
            messagingTemplate.convertAndSend(destination, recallNotification);

            log.info("✅ 圈子消息撤回成功并已推送：messageId={}, circleId={}", 
                    recallRequest.getMessageId(), message.getCircleId());

        } catch (Exception e) {
            log.error("处理圈子消息撤回失败", e);
        }
    }

    /**
     * 处理删除消息（预留接口，后续实现）
     */
    @MessageMapping("/circle-delete-message")
    public void deleteCircleMessage(@Payload DeleteMessageRequest deleteRequest) {
        log.info("收到删除请求（功能待实现）：messageId={}, userId={}", 
                deleteRequest.getMessageId(), deleteRequest.getUserId());
        // TODO: 后续实现删除功能
    }
}
