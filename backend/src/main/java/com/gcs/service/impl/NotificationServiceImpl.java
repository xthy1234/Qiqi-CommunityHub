package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.NotificationDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.Notification;
import com.gcs.entity.User;
import com.gcs.enums.NotificationType;
import com.gcs.service.NotificationService;
import com.gcs.service.UserService;
import com.gcs.utils.MPUtil;
import com.gcs.utils.PageUtils;
import com.gcs.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 */
@Slf4j
@Service("notificationService")
public class NotificationServiceImpl extends ServiceImpl<NotificationDao, Notification> implements NotificationService {

    @Autowired
    private NotificationDao notificationDao;
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createNotification(Long userId, Integer type, Long sourceId, Map<String, Object> content, Map<String, Object> extra) {

        if (content != null && !content.isEmpty()) {
            log.warn(" content 参数已废弃，请使用 extra 字段传递通知数据。userId: {}, type: {}", userId, type);
        }
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setSourceId(sourceId);

        notification.setContent(null);

        notification.setExtra(extra);
        notification.setIsRead(false);

        baseMapper.insert(notification);

        log.info("创建通知成功，userId: {}, type: {}, extra: {}", userId, type, extra);
        
        // 立即推送 WebSocket 消息
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
            log.info("[WebSocket 推送] 推送到队列：{}", destination);
            
            messagingTemplate.convertAndSendToUser(
                String.valueOf(notification.getUserId()),
                "/queue/notification",
                message
            );
            
            log.info(" [WebSocket 推送] 推送完成：notificationId={}, userId={}", 
                     notification.getId(), notification.getUserId());
                     
        } catch (Exception e) {
            log.error("[WebSocket 推送] 失败，notificationId: {}", notification.getId(), e);
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
        // 修复：使用 XML 中定义的分页查询方法，确保 extra 字段正确反序列化
        int offset = (page - 1) * limit;
        
        List<Notification> notifications = notificationDao.selectByUserIdPage(userId, isRead, offset, limit);
        
        // 添加调试日志
        log.info("[查询通知] userId: {}, isRead: {}, page: {}, limit: {}, 查询结果数量：{}",
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
        
        // 推送已读状态更新
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
            log.info("[已读状态推送] 推送到队列：/user/{}/queue/notification", userId);
            messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notification",
                message
            );
            
            log.info(" [已读状态推送] 推送完成：userId={}, count={}", userId, notificationIds.size());
            
        } catch (Exception e) {
            log.error("[已读状态推送] 失败，userId: {}", userId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(Long userId) {
        notificationDao.clearAll(userId);
        log.info("清空通知成功，userId: {}", userId);
        
        // 推送清空消息
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
            
            log.info("[清空推送] 推送到队列：/user/{}/queue/notification", userId);
            messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notification",
                message
            );
            
            log.info(" [清空推送] 推送完成：userId={}", userId);
            
        } catch (Exception e) {
            log.error("[清空推送] 失败，userId: {}", userId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sendBatchNotifications(List<Long> userIds, Integer type, Map<String, Object> content, Map<String, Object> extra) {
        if (type == null) {
            throw new IllegalArgumentException("通知类型不能为空");
        }

        List<Long> targetUserIds = userIds;
        
        if (CollectionUtils.isEmpty(userIds)) {
            List<User> allUsers = userDao.selectList(new QueryWrapper<User>().select("id"));
            targetUserIds = allUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(targetUserIds)) {
            log.warn("没有目标用户，跳过发送通知");
            return 0;
        }

        List<Notification> notifications = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long uid : targetUserIds) {
            Notification notification = new Notification();
            notification.setUserId(uid);
            notification.setType(type);
            notification.setContent(null);
            notification.setExtra(extra);
            notification.setIsRead(false);
            notification.setCreateTime(now);
            notifications.add(notification);
        }

        final int BATCH_SIZE = 1000;
        int sentCount = 0;
        for (int i = 0; i < notifications.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, notifications.size());
            List<Notification> batch = notifications.subList(i, end);
            boolean result = this.saveBatch(batch);
            if (result) {
                sentCount += batch.size();
                
                for (Notification notification : batch) {
                    pushNotificationToUser(notification);
                }
            }
        }

        log.info("批量发送通知完成，目标用户数：{}, 实际发送：{}, 通知类型：{}", 
                 targetUserIds.size(), sentCount, type);
        return sentCount;
    }

    @Override
    public int sendBroadcastNotification(Integer type, Map<String, Object> content, Map<String, Object> extra) {
        return sendBatchNotifications(null, type, content, extra);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawNotification(Long notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("通知 ID 不能为空");
        }

        return this.removeById(notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchWithdrawNotifications(List<Long> notificationIds) {
        if (CollectionUtils.isEmpty(notificationIds)) {
            throw new IllegalArgumentException("通知 ID 列表不能为空");
        }

        int removedCount = this.removeByIds(notificationIds) ? notificationIds.size() : 0;
        log.info("批量撤回通知，撤回数量：{}", removedCount);
        return removedCount;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        return queryPage(params, wrapper);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Notification> queryWrapper) {
        IPage<Notification> page = this.page(new Page<>(), queryWrapper);
        return new PageUtils(page);
    }

}
