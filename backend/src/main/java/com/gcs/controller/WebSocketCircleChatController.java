package com.gcs.controller;

import com.gcs.dao.UserDao;
import com.gcs.entity.User;
import com.gcs.vo.CircleChatMessage;
import com.gcs.dto.DeleteMessageRequest;
import com.gcs.dto.RecallMessageRequest;
import com.gcs.entity.CircleChat;
import com.gcs.service.CircleChatService;
import com.gcs.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 圈子聊天 WebSocket 控制器
 * 处理实时消息发送和撤回
 */
@Slf4j
@Controller
public class WebSocketCircleChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CircleChatService circleChatService;

    @Autowired
    private UserDao userDao;

    /**
     * 构造函数日志
     */
    public WebSocketCircleChatController() {
        log.info("=== CircleChatWebSocketController 已初始化 ===");
    }

    /**
     * 处理圈子消息发送
     */
    @MessageMapping("/circle-message")
    public void sendCircleMessage(@Payload CircleChatMessage chatMessage, 
                                   StompHeaderAccessor accessor) {
        // ✅ 从 Session 获取真实用户 ID
        Long currentUserId = (Long) accessor.getSessionAttributes().get("userId");
        
        log.info("🚀 [圈子消息] 收到 WebSocket 消息：circleId={}, sessionUserId={}", 
                 chatMessage.getCircleId(), currentUserId);

        try {
            // 🔒 验证用户是否已登录
            if (currentUserId == null) {
                log.error("❌ [圈子消息] 发送失败：用户未登录或 Session 已过期");
                return;
            }

            // 🔒 验证必填字段
            if (chatMessage.getCircleId() == null) {
                log.error("❌ [圈子消息] 发送失败：circleId 不能为空");
                return;
            }

            if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                log.error("❌ [圈子消息] 发送失败：消息内容不能为空");
                return;
            }

            // 🔒 验证消息类型合法性
            if (chatMessage.getMsgType() != null && 
                (chatMessage.getMsgType() < 0 || chatMessage.getMsgType() > 2)) {
                log.error("❌ [圈子消息] 发送失败：不支持的消息类型 {}", chatMessage.getMsgType());
                return;
            }
            
            // 5. 转换为 WebSocket 消息对象
            CircleChatMessage messageVO = circleChatService.sendMessage(
                chatMessage.getCircleId(),
                currentUserId,
                chatMessage.getContent(),
                chatMessage.getMsgType()
            );


            messageVO.setIsSelf(null);

            log.info("✅ [圈子消息] 消息已发送：messageId={}, senderId={}", 
                     messageVO.getId(), messageVO.getSenderId());

            // 广播给圈子所有成员
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
    public void recallCircleMessage(@Payload RecallMessageRequest recallRequest,
                                     StompHeaderAccessor accessor) {
        try {
            // ✅ 从 Session 获取真实用户 ID
            Long currentUserId = (Long) accessor.getSessionAttributes().get("userId");
            
            if (currentUserId == null) {
                log.error("❌ [撤回消息] 失败：用户未登录或 Session 已过期");
                return;
            }

            log.info("收到撤回请求：messageId={}, requestUserId={}, sessionUserId={}", 
                    recallRequest.getMessageId(), recallRequest.getUserId(), currentUserId);

            // 🔒 使用 Session 中的真实用户 ID（忽略前端传来的 userId）
            boolean success = circleChatService.recallMessage(
                recallRequest.getMessageId(), 
                currentUserId  // 使用 Session 中的 ID，安全可靠
            );

            if (!success) {
                log.warn("撤回失败：messageId={}, userId={}", 
                        recallRequest.getMessageId(), currentUserId);
                return;
            }

            // 查询撤回后的消息信息
            CircleChat message = circleChatService.getById(recallRequest.getMessageId());
            if (message == null) {
                return;
            }

            // 组装撤回通知消息
            CircleChatMessage recallNotification = new CircleChatMessage();
            recallNotification.setId(message.getId());
            recallNotification.setCircleId(message.getCircleId());
            recallNotification.setSenderId(currentUserId);  // 使用 Session 中的 ID
            recallNotification.setAction("RECALL");
            recallNotification.setContent(""); // 撤回后内容为空

            // 广播给圈子所有成员
            String destination = "/topic/circles/" + message.getCircleId() + "/messages";
            messagingTemplate.convertAndSend(destination, recallNotification);

            log.info("✅ 圈子消息撤回成功并已推送：messageId={}, circleId={}", 
                    recallRequest.getMessageId(), message.getCircleId());

        } catch (Exception e) {
            log.error("处理圈子消息撤回失败", e);
        }
    }

    /**
     * 处理删除消息（管理员/圈主权限）
     */
    @MessageMapping("/circle-delete-message")
    public void deleteCircleMessage(@Payload DeleteMessageRequest deleteRequest,
                                     StompHeaderAccessor accessor) {
        try {
            // ✅ 从 Session 获取真实用户 ID
            Long currentUserId = (Long) accessor.getSessionAttributes().get("userId");
            
            if (currentUserId == null) {
                log.error("❌ [删除消息] 失败：用户未登录或 Session 已过期");
                return;
            }

            log.info("收到删除请求：messageId={}, requestUserId={}, sessionUserId={}", 
                    deleteRequest.getMessageId(), deleteRequest.getUserId(), currentUserId);

            // 🔒 使用 Session 中的真实用户 ID
            boolean success = circleChatService.deleteMessage(
                deleteRequest.getMessageId(), 
                currentUserId
            );

            if (!success) {
                log.warn("删除失败：messageId={}, userId={}", 
                        deleteRequest.getMessageId(), currentUserId);
                return;
            }

            // 查询删除后的消息信息
            CircleChat message = circleChatService.getById(deleteRequest.getMessageId());
            if (message == null) {
                return;
            }

            // 🔥 获取删除者信息（用于推送）
            User deleter = userDao.selectById(currentUserId);

            // 组装删除通知消息
            CircleChatMessage deleteNotification = new CircleChatMessage();
            deleteNotification.setId(message.getId());
            deleteNotification.setCircleId(message.getCircleId());
            deleteNotification.setSenderId(message.getSenderId());  // 原消息发送者 ID
            deleteNotification.setAction("DELETE");
            deleteNotification.setContent(""); // 删除后内容为空
            deleteNotification.setDeletedByAdmin(true);
            
            // 🔥 设置删除者信息（UserSimpleVO 格式）
            if (deleter != null) {
                UserSimpleVO deleterVO = new UserSimpleVO();
                deleterVO.setId(deleter.getId());
                deleterVO.setNickname(deleter.getNickname());
                deleterVO.setAvatar(deleter.getAvatar());
                deleteNotification.setDeleter(deleterVO);  // 👈 使用 deleter 字段
            }
            
            deleteNotification.setDeletedTime(message.getDeletedTime());

            // 广播给圈子所有成员
            String destination = "/topic/circles/" + message.getCircleId() + "/messages";
            messagingTemplate.convertAndSend(destination, deleteNotification);

            log.info("✅ 圈子消息已删除并推送：messageId={}, circleId={}, deletedBy={}", 
                    deleteRequest.getMessageId(), message.getCircleId(), currentUserId);

        } catch (Exception e) {
            log.error("处理圈子消息删除失败", e);
        }
    }
}
