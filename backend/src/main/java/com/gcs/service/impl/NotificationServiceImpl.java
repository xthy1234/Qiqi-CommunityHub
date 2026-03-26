package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.NotificationDao;
import com.gcs.entity.Notification;
import com.gcs.enums.NotificationType;
import com.gcs.service.NotificationService;
import com.gcs.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务实现类
 */
@Slf4j
@Service("notificationService")
public class NotificationServiceImpl extends ServiceImpl<NotificationDao, Notification> 
        implements NotificationService {

    @Autowired
    private NotificationDao notificationDao;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createNotification(Long userId, Integer type, Long sourceId, Map<String, Object> content, Map<String, Object> extra) {
        // ⚠️ content 参数已废弃，所有数据应通过 extra 传递
        if (content != null && !content.isEmpty()) {
            log.warn("⚠️ content 参数已废弃，请使用 extra 字段传递通知数据。userId: {}, type: {}", userId, type);
        }
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setSourceId(sourceId);
        // content 字段不再使用，设置为 null 或空 Map
        notification.setContent(null);
        // ✅ 所有数据都存储在 extra 字段
        notification.setExtra(extra);
        notification.setIsRead(false);

        baseMapper.insert(notification);

        log.info("创建通知成功，userId: {}, type: {}, extra: {}", userId, type, extra);
        
        // 🚀 立即推送 WebSocket 消息
        pushNotificationToUser(notification);
        
        return notification;
    }
    
    /**
     * 向用户推送通知消息
     * 
     * @param notification 通知实体
     */
    private void pushNotificationToUser(Notification notification) {
        try {
            // 转换为 VO
            NotificationVO notificationVO = convertToVO(notification);
            
            // 组装 WebSocket 消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTIFICATION");
            message.put("data", notificationVO);
            
            // 推送到用户专属队列
            String destination = "/user/" + notification.getUserId() + "/queue/notification";
            log.info("📤 [WebSocket 推送] 推送到队列：{}", destination);
            
            messagingTemplate.convertAndSendToUser(
                String.valueOf(notification.getUserId()),
                "/queue/notification",
                message
            );
            
            log.info("✅ [WebSocket 推送] 推送完成：notificationId={}, userId={}", 
                     notification.getId(), notification.getUserId());
                     
        } catch (Exception e) {
            log.error("❌ [WebSocket 推送] 失败，notificationId: {}", notification.getId(), e);
            // 不抛出异常，避免影响通知创建流程
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
        // content 字段已废弃，不再设置
        vo.setContent(null);
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

    @Override
    public List<Notification> getUserNotifications(Long userId, Boolean isRead, Integer page, Integer limit) {
        // 🔧 修复：使用 XML 中定义的分页查询方法，确保 extra 字段正确反序列化
        int offset = (page - 1) * limit;
        
        List<Notification> notifications = notificationDao.selectByUserIdPage(userId, isRead, offset, limit);
        
        // 🔍 添加调试日志
        log.info("📋 [查询通知] userId: {}, isRead: {}, page: {}, limit: {}, 查询结果数量：{}", 
                 userId, isRead, page, limit, notifications.size());
        for (Notification notification : notifications) {
            log.info("  - notificationId: {}, type: {}, extra={}", 
                     notification.getId(), notification.getType(), notification.getExtra());
        }

        return notifications;
    }

    @Override
    public long countUnread(Long userId) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("is_read", false);
        return baseMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, List<Long> notificationIds) {
        notificationDao.markAsRead(userId, notificationIds);
        log.info("标记通知已读成功，userId: {}, count: {}", userId, notificationIds.size());
        
        // 🚀 推送已读状态更新
        pushReadStatusToUpdate(userId, notificationIds);
    }
    
    /**
     * 推送已读状态更新
     */
    private void pushReadStatusToUpdate(Long userId, List<Long> notificationIds) {
        try {
            // 组装状态更新消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTIFICATION_READ_UPDATE");
            message.put("data", Map.of(
                "notificationIds", notificationIds,
                "isRead", true,
                "timestamp", System.currentTimeMillis()
            ));
            
            // 推送到用户专属队列
            log.info("📤 [已读状态推送] 推送到队列：/user/{}/queue/notification", userId);
            messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notification",
                message
            );
            
            log.info("✅ [已读状态推送] 推送完成：userId={}, count={}", userId, notificationIds.size());
            
        } catch (Exception e) {
            log.error("❌ [已读状态推送] 失败，userId: {}", userId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(Long userId) {
        notificationDao.clearAll(userId);
        log.info("清空通知成功，userId: {}", userId);
        
        // 🚀 推送清空消息
        pushClearNotification(userId);
    }
    
    /**
     * 推送清空通知消息
     */
    private void pushClearNotification(Long userId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTIFICATION_CLEARED");
            message.put("data", Map.of(
                "timestamp", System.currentTimeMillis()
            ));
            
            log.info("📤 [清空推送] 推送到队列：/user/{}/queue/notification", userId);
            messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notification",
                message
            );
            
            log.info("✅ [清空推送] 推送完成：userId={}", userId);
            
        } catch (Exception e) {
            log.error("❌ [清空推送] 失败，userId: {}", userId, e);
        }
    }
}
