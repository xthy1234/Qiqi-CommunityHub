package com.gcs.controller;

import com.gcs.vo.ChatMessage;
import com.gcs.dto.DeleteMessageRequest;
import com.gcs.dto.QueryUserOnlineRequest;
import com.gcs.dto.ReadReceipt;
import com.gcs.dto.RecallMessageRequest;
import com.gcs.entity.PrivateMessage;
import com.gcs.entity.User;
import com.gcs.enums.MessageStatus;
import com.gcs.service.PrivateMessageService;
import com.gcs.service.UserService;
import com.gcs.service.UserOnlineStatusService;
import com.gcs.vo.OnlineStatusVO;
import com.gcs.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 聊天控制器
 * 处理实时消息发送和已读回执
 */
@Slf4j
@Controller
public class WebSocketChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PrivateMessageService privateMessageService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserOnlineStatusService onlineStatusService;
    
    // 添加构造函数日志
    public WebSocketChatController() {
        log.info("=== WebSocketChatController 已初始化 ===");
    }

    /**
     * 处理私聊消息发送
     */
    @MessageMapping("/private-message")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, StompHeaderAccessor accessor) {
        try {
            // ✅ 从 Session 获取真实用户 ID
            Long currentUserId = (Long) accessor.getSessionAttributes().get("userId");
            
            log.info("🚀 [私聊消息] 收到 WebSocket 消息：from={}, to={}", 
                    chatMessage.getFromUserId(), chatMessage.getToUserId());

            // 🔒 验证用户是否已登录
            if (currentUserId == null) {
                log.error("❌ [私聊消息] 发送失败：用户未登录或 Session 已过期");
                throw new IllegalStateException("用户未登录");
            }

            // 🔥 1. 验证必填字段 - content 不能为空
            if (chatMessage.getContent() == null || chatMessage.getContent().isEmpty()) {
                log.error("❌ [私聊消息] 发送失败：消息内容不能为空，from={}, to={}", 
                         chatMessage.getFromUserId(), chatMessage.getToUserId());
                throw new IllegalArgumentException("消息内容不能为空");
            }
            
            // 🔥 2. 如果 msgType 为空，设置默认值
            if (chatMessage.getMsgType() == null) {
                chatMessage.setMsgType(0); // 默认为文本类型
                log.warn("⚠️ [私聊消息] msgType 为空，使用默认值 0");
            }

            // 🔒 验证其他必填字段
            if (chatMessage.getToUserId() == null) {
                log.error("❌ [私聊消息] 发送失败：toUserId 不能为空");
                throw new IllegalArgumentException("接收方用户 ID 不能为空");
            }

            // 保存消息到数据库
            PrivateMessage message = new PrivateMessage();
            message.setFromUserId(currentUserId);  // ✅ 使用 Session 中的真实 ID
            message.setToUserId(chatMessage.getToUserId());
            message.setContent(chatMessage.getContent());  // ✅ 确保不为 null
            message.setMsgType(chatMessage.getMsgType());
            message.setStatus(MessageStatus.UNREAD);

            privateMessageService.save(message);
            log.info("✅ 消息已保存到数据库，messageId={}", message.getId());

            // 2. 查询发送方用户信息
            User fromUser = userService.getById(chatMessage.getFromUserId());
            UserSimpleVO fromUserVO = convertToUserSimpleVO(fromUser);
            log.debug("发送方用户信息：{}", fromUserVO);

            // 3. 查询接收方用户信息（可选）
            User toUser = userService.getById(chatMessage.getToUserId());
            UserSimpleVO toUserVO = convertToUserSimpleVO(toUser);
            log.debug("接收方用户信息：{}", toUserVO);

            // 4. 组装完整的消息 VO
            ChatMessage messageVO = new ChatMessage();
            messageVO.setId(message.getId());
            messageVO.setFromUserId(message.getFromUserId());
            messageVO.setFromUser(fromUserVO);
            messageVO.setToUserId(message.getToUserId());
            messageVO.setToUser(toUserVO);
            messageVO.setContent(message.getContent());
            messageVO.setMsgType(message.getMsgType());
            messageVO.setStatus(message.getStatus().getCode());
            messageVO.setCreateTime(message.getCreateTime());
            messageVO.setAction("SEND");

            log.info("📦 准备推送消息");
            
            // 👇 明确指定完整路径
            String receiverPath = "/user/" + chatMessage.getToUserId() + "/queue/private-messages";
            String senderPath = "/user/" + chatMessage.getFromUserId() + "/queue/private-messages";

            log.info("推送到接收方：{}", receiverPath);

            log.info("推送到发送方：{}", senderPath);
            
            // 直接发送到完整路径
            messageVO.setIsSelf(false);
            messagingTemplate.convertAndSend(receiverPath, messageVO);
            messageVO.setIsSelf(true);
            messagingTemplate.convertAndSend(senderPath, messageVO);
            
            log.info("✅ 推送完成");

            log.info("🎉 消息推送完成，接收方：{}", chatMessage.getToUserId());

        } catch (IllegalArgumentException e) {
            log.error("❌ [私聊消息] 参数错误：{}", e.getMessage());
            throw e;  // 抛出异常让前端知道错误
        } catch (Exception e) {
            log.error("❌ [私聊消息] 发送失败", e);
            throw e;  // 抛出异常让前端知道错误
        }
    }

    /**
     * 处理已读回执
     */
    @MessageMapping("/read-receipt")
    public void handleReadReceipt(@Payload ReadReceipt receipt) {
        try {
            log.info("收到已读回执：from={} to={}", receipt.getFromUserId(), receipt.getToUserId());

            // ✅ 添加详细日志，帮助调试
            //由于fromid是标记了toId发送的信息，所以这里fromid是消息的接收方，toid是消息的发送方
            log.info("📖 [已读回执] 用户 {} 标记了来自用户 {} 的消息为已读",
                    receipt.getFromUserId()  , receipt.getToUserId());

            // 1. 更新数据库中该对话的消息状态为已读
            boolean success = privateMessageService.markAsRead(
                    receipt.getFromUserId(),
                    receipt.getToUserId()
            );

            // ✅ 添加成功/失败的详细日志
            if (success) {
                log.info("✅ [已读回执] 更新成功：用户 {} -> 用户 {}", 
                        receipt.getFromUserId(), receipt.getToUserId());
            } else {
                log.warn("⚠️ [已读回执] 更新失败或无需更新：用户 {} -> 用户 {}，可能原因：", 
                        receipt.getFromUserId(), receipt.getToUserId());
                log.warn("   1. 没有未读消息");
                log.warn("   2. 参数可能传反了（fromUserId 应该是阅读者，toUserId 应该是消息发送者）");
                
                // ✅ 查询一下实际有多少未读消息
                Integer unreadCount = privateMessageService.countUnreadMessages(
                    receipt.getFromUserId(), 
                    receipt.getToUserId()
                );
                log.warn("   📊 当前未读消息数：{}", unreadCount);
            }

            // 2. 将已读回执推送给消息发送方，让他知道对方已读
            messagingTemplate.convertAndSendToUser(
                    receipt.getToUserId().toString(),
                    "/queue/read-receipts",
                    receipt
            );

            log.info("已读回执已推送给用户：{}, 更新结果：{}", receipt.getToUserId(), success ? "成功" : "失败");

        } catch (Exception e) {
            log.error("处理已读回执失败", e);
        }
    }

    /**
     * 处理撤回消息
     */
    @MessageMapping("/recall-message")
    public void recallMessage(@Payload RecallMessageRequest recallRequest) {
        try {
            log.info("收到撤回请求：messageId={}, userId={}", recallRequest.getMessageId(), recallRequest.getUserId());

            // 1. 执行撤回操作
            boolean success = privateMessageService.recallMessage(
                recallRequest.getMessageId(), 
                recallRequest.getUserId()
            );

            if (!success) {
                log.warn("撤回失败：messageId={}, userId={}", recallRequest.getMessageId(), recallRequest.getUserId());
                return;
            }

            // 2. 查询撤回后的消息信息
            PrivateMessage message = privateMessageService.getById(recallRequest.getMessageId());
            if (message == null) {
                return;
            }

            // 3. 组装撤回通知消息
            RecallMessageRequest recallNotification = new RecallMessageRequest();
            recallNotification.setMessageId(message.getId());
            recallNotification.setUserId(recallRequest.getUserId());
            recallNotification.setReason(recallRequest.getReason());

            // 4. 推送给接收方（通知消息已被撤回）
            messagingTemplate.convertAndSendToUser(
                    message.getToUserId().toString(),
                    "/queue/message-recall",
                    recallNotification
            );

            // 5. 也推送给发送方自己（确认撤回成功）
            messagingTemplate.convertAndSendToUser(
                    message.getFromUserId().toString(),
                    "/queue/message-recall",
                    recallNotification
            );

            log.info("消息撤回成功并已推送：messageId={}", recallRequest.getMessageId());

        } catch (Exception e) {
            log.error("处理撤回消息失败", e);
        }
    }

    /**
     * 处理删除消息
     */
    @MessageMapping("/delete-message")
    public void deleteMessage(@Payload DeleteMessageRequest deleteRequest) {
        try {
            log.info("收到删除请求：messageId={}, userId={}", 
                     deleteRequest.getMessageId(), deleteRequest.getUserId());

            // 1. 执行删除操作
            boolean success = privateMessageService.deleteMessage(
                deleteRequest.getMessageId(), 
                deleteRequest.getUserId()
            );

            if (!success) {
                log.warn("删除失败：messageId={}, userId={}", 
                        deleteRequest.getMessageId(), deleteRequest.getUserId());
                return;
            }

            // 2. 查询消息信息
            PrivateMessage message = privateMessageService.getById(deleteRequest.getMessageId());
            if (message == null) {
                return;
            }

            // 3. 组装删除通知（推送给自己，用于同步删除状态）
            DeleteMessageRequest notification = new DeleteMessageRequest();
            notification.setMessageId(message.getId());
            notification.setBothDeleted(false);  // 单向删除，永远为 false

            // 4. 推送给删除者自己（确认删除成功，前端可以移除该消息）
            messagingTemplate.convertAndSendToUser(
                    deleteRequest.getUserId().toString(),
                    "/queue/message-delete",
                    notification
            );

            log.info("消息删除确认已推送：messageId={}, 目标用户：{}", 
                    deleteRequest.getMessageId(), deleteRequest.getUserId());

        } catch (Exception e) {
            log.error("处理删除消息失败", e);
        }
    }
    
    /**
     * 处理在线状态查询
     */
    @MessageMapping("/query-user-online-status")
    public void queryUserOnlineStatus(@Payload QueryUserOnlineRequest request, StompHeaderAccessor accessor) {
        try {
            // 🎯 从 Session 中获取当前登录用户 ID
            Long currentUserId = (Long) accessor.getSessionAttributes().get("userId");
            
            log.info("📊 [在线状态] 收到查询请求，request.userIds={}, currentUserId={}", 
                    request.getUserIds(), currentUserId);
            
            if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                log.warn("⚠️ [在线状态] 查询的用户 ID 列表为空");
                return;
            }
            
            // 批量查询用户在线状态
            List<OnlineStatusVO> statusList = new ArrayList<>();
            for (Long userId : request.getUserIds()) {
                boolean isOnline = onlineStatusService.isOnline(userId);
                
                OnlineStatusVO vo = new OnlineStatusVO();
                vo.setUserId(userId);
                vo.setIsOnline(isOnline);
                vo.setTimestamp(System.currentTimeMillis());
                
                statusList.add(vo);
                
                log.info("📊 [在线状态] 用户 {} 在线状态：{}", userId, isOnline ? "在线" : "离线");
            }
            
            // 📤 将结果推送回发起查询的客户端
            if (currentUserId != null) {
                String replyDestination = "/user/" + currentUserId + "/queue/user-online-status";
                log.info("📤 [在线状态] 推送查询结果到：{}, 结果数：{}", replyDestination, statusList.size());
                
                messagingTemplate.convertAndSend(replyDestination, statusList);
                log.info("✅ [在线状态] 已推送查询结果");
            } else {
                log.error("❌ [在线状态] 无法推送结果：未找到当前用户 Session，accessor.sessionId={}", 
                        accessor.getSessionId());
            }
            
        } catch (Exception e) {
            log.error("❌ [在线状态] 查询失败", e);
        }
    }
    
    /**
     * 将 User 转换为 UserSimpleVO
     */
    private UserSimpleVO convertToUserSimpleVO(User user) {
        if (user == null) {
            return null;
        }
        UserSimpleVO vo = new UserSimpleVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
