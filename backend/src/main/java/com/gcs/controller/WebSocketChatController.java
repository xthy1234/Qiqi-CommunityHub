package com.gcs.controller;

import com.gcs.dto.ChatMessage;
import com.gcs.dto.ReadReceipt;
import com.gcs.dto.RecallMessageRequest;
import com.gcs.entity.PrivateMessage;
import com.gcs.entity.User;
import com.gcs.enums.MessageStatus;
import com.gcs.service.PrivateMessageService;
import com.gcs.service.UserService;
import com.gcs.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

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

    /**
     * 处理私聊消息发送
     */
    @MessageMapping("/private-message")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        try {
            log.info("收到 WebSocket 消息：from={} to={}", chatMessage.getFromUserId(), chatMessage.getToUserId());

            // 1. 保存消息到数据库
            PrivateMessage message = new PrivateMessage();
            message.setFromUserId(chatMessage.getFromUserId());
            message.setToUserId(chatMessage.getToUserId());
            message.setContent(chatMessage.getContent());
            message.setMsgType(chatMessage.getMsgType());
            message.setStatus(MessageStatus.UNREAD);
            message.setCreateTime(LocalDateTime.now());

            privateMessageService.save(message);

            // 2. 查询发送方用户信息
            User fromUser = userService.getById(chatMessage.getFromUserId());
            UserSimpleVO fromUserVO = convertToUserSimpleVO(fromUser);

            // 3. 查询接收方用户信息（可选）
            User toUser = userService.getById(chatMessage.getToUserId());
            UserSimpleVO toUserVO = convertToUserSimpleVO(toUser);

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

            // 5. 推送给接收方
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getToUserId().toString(),
                    "/queue/private-messages",
                    messageVO
            );

            // 6. 也推送给发送方自己（用于更新界面）
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getFromUserId().toString(),
                    "/queue/private-messages",
                    messageVO
            );

            log.info("消息已推送给用户：{}", chatMessage.getToUserId());

        } catch (Exception e) {
            log.error("发送 WebSocket 消息失败", e);
        }
    }

    /**
     * 处理已读回执
     */
    @MessageMapping("/read-receipt")
    public void handleReadReceipt(@Payload ReadReceipt receipt) {
        try {
            log.info("收到已读回执：from={} to={}", receipt.getFromUserId(), receipt.getToUserId());

            // 1. 更新数据库中该对话的消息状态为已读
            boolean success = privateMessageService.markAsRead(receipt.getFromUserId(), receipt.getToUserId());

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
