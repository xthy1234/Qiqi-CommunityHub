package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.NotificationDao;
import com.gcs.entity.Notification;
import com.gcs.enums.NotificationType;
import com.gcs.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 通知服务实现类
 */
@Slf4j
@Service("notificationService")
public class NotificationServiceImpl extends ServiceImpl<NotificationDao, Notification> 
        implements NotificationService {

    private NotificationDao notificationDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createNotification(Long userId, Integer type, Long sourceId, Map<String, Object> content, Map<String, Object> extra) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setSourceId(sourceId);
        notification.setContent(content);
        notification.setContent(extra);
        notification.setIsRead(false);

        baseMapper.insert(notification);

        log.info("创建通知成功，userId: {}, type: {}", userId, type);
        return notification;
    }

    @Override
    public List<Notification> getUserNotifications(Long userId, Boolean isRead, Integer page, Integer limit) {
        Page<Notification> mpPage = new Page<>(page, limit);
        
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (isRead != null) {
            wrapper.eq("is_read", isRead);
        }
        wrapper.orderByDesc("create_time");

        return baseMapper.selectPage(mpPage, wrapper).getRecords();
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(Long userId) {
        notificationDao.clearAll(userId);
        log.info("清空通知成功，userId: {}", userId);
    }
}
