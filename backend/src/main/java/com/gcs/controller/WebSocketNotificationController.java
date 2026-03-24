package com.gcs.controller;

import com.gcs.entity.Notification;
import com.gcs.enums.NotificationType;
import com.gcs.service.NotificationService;
import com.gcs.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知 WebSocket 控制器
 * 处理实时通知推送
 */
@Slf4j
@Controller
public class WebSocketNotificationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    /**
     * 构造函数日志
     */
    public WebSocketNotificationController() {
        log.info("=== WebSocketNotificationController 已初始化 ===");
    }

    /**
     * 手动触发通知推送（用于测试或其他模块调用）
     * 
     * @param notificationRequest 通知请求数据
     * @param accessor STOMP 头信息
     */
    @MessageMapping("/notification-trigger")
    public void triggerNotification(@Payload Map<String, Object> notificationRequest,
                                    StompHeaderAccessor accessor) {
        try {
            // ✅ 从 Session 获取真实用户 ID
            Long currentUserId = (Long) accessor.getSessionAttributes().get("userId");
            
            log.info("🚀 [通知推送] 收到触发请求：userId={}, request={}", 
                     currentUserId, notificationRequest);

            if (currentUserId == null) {
                log.error("❌ [通知推送] 失败：用户未登录或 Session 已过期");
                return;
            }

            // 🔒 验证必填字段
            Integer type = (Integer) notificationRequest.get("type");
            String contentStr = (String) notificationRequest.get("content");
            Long targetUserId = ((Number) notificationRequest.get("targetUserId")).longValue();
            Long sourceId = notificationRequest.get("sourceId") != null 
                ? ((Number) notificationRequest.get("sourceId")).longValue() 
                : null;

            if (type == null || targetUserId == null) {
                log.error("❌ [通知推送] 失败：缺少必填字段（type 或 targetUserId）");
                return;
            }

            // 解析 content（可能是字符串或 Map）
            Map<String, Object> content = parseContent(contentStr);
            Map<String, Object> extra = new HashMap<>();
            if (notificationRequest.get("extra") instanceof Map) {
                extra = (Map<String, Object>) notificationRequest.get("extra");
            }

            // 📝 创建通知记录
            Notification notification = notificationService.createNotification(
                targetUserId,
                type,
                sourceId,
                content,
                extra
            );

            // 📨 转换为 VO
            NotificationVO notificationVO = convertToVO(notification);

            // 🔔 组装 WebSocket 消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTIFICATION");
            message.put("data", notificationVO);

            // 📤 推送到用户专属队列
            String destination = "/user/" + targetUserId + "/queue/notification";
            log.info("📤 [通知推送] 推送到队列：{}", destination);

            messagingTemplate.convertAndSendToUser(
                String.valueOf(targetUserId),
                "/queue/notification",
                message
            );

            log.info("✅ [通知推送] 推送完成：notificationId={}, targetUserId={}", 
                     notification.getId(), targetUserId);

        } catch (Exception e) {
            log.error("❌ [通知推送] 失败", e);
            log.error("   错误类型：{}", e.getClass().getName());
            log.error("   错误消息：{}", e.getMessage());
        }
    }

    /**
     * 批量标记通知已读后，推送状态更新
     * 
     * @param markReadRequest 已读请求数据
     * @param accessor STOMP 头信息
     */
    @MessageMapping("/notification-mark-read")
    public void markNotificationAsRead(@Payload Map<String, Object> markReadRequest,
                                       StompHeaderAccessor accessor) {
        try {
            // ✅ 从 Session 获取真实用户 ID
            Long currentUserId = (Long) accessor.getSessionAttributes().get("userId");
            
            if (currentUserId == null) {
                log.error("❌ [标记已读] 失败：用户未登录或 Session 已过期");
                return;
            }

            @SuppressWarnings("unchecked")
            var notificationIds = (java.util.List<Long>) markReadRequest.get("notificationIds");

            if (notificationIds == null || notificationIds.isEmpty()) {
                log.error("❌ [标记已读] 失败：notificationIds 不能为空");
                return;
            }

            // 📝 执行标记已读操作
            notificationService.markAsRead(currentUserId, notificationIds);

            // 🔔 组装状态更新消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTIFICATION_READ_UPDATE");
            message.put("data", Map.of(
                "notificationIds", notificationIds,
                "isRead", true,
                "timestamp", System.currentTimeMillis()
            ));

            // 📤 推送到用户专属队列
            log.info("📤 [标记已读] 推送到队列：/user/{}/queue/notification", currentUserId);
            messagingTemplate.convertAndSendToUser(
                String.valueOf(currentUserId),
                "/queue/notification",
                message
            );

            log.info("✅ [标记已读] 推送完成：userId={}, count={}", 
                     currentUserId, notificationIds.size());

        } catch (Exception e) {
            log.error("❌ [标记已读] 失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析 content 字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(String contentStr) {
        if (contentStr == null || contentStr.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // 尝试解析 JSON 字符串
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(contentStr, Map.class);
        } catch (Exception e) {
            // 如果解析失败，返回简单结构
            Map<String, Object> content = new HashMap<>();
            content.put("text", contentStr);
            return content;
        }
    }

    /**
     * 转换为 VO
     */
    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setUserId(notification.getUserId());
        vo.setType(notification.getType());
        vo.setSourceId(notification.getSourceId());
        vo.setContent(notification.getContent());
        vo.setExtra(notification.getExtra());
        vo.setIsRead(notification.getIsRead());
        vo.setCreateTime(notification.getCreateTime());

        // 设置类型描述
        try {
            NotificationType type = NotificationType.valueOfCode(notification.getType());
            vo.setTypeName(type.getDescription());
        } catch (IllegalArgumentException e) {
            vo.setTypeName("未知类型");
        }

        return vo;
    }
}
