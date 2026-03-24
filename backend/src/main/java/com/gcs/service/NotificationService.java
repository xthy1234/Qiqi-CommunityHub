package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Notification;

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 创建通知
     *
     * @param userId 用户 ID
     * @param type 通知类型
     * @param sourceId 业务 ID
     * @param content 通知内容（JSON）
     * @param extra 额外内容（JSON）
     * @return 通知实体
     */
    Notification createNotification(Long userId, Integer type, Long sourceId, Map<String, Object> content, Map<String, Object> extra);

    /**
     * 获取用户通知列表（分页）
     *
     * @param userId 用户 ID
     * @param isRead 是否已读（null 表示全部）
     * @param page 页码
     * @param limit 每页数量
     * @return 通知列表
     */
    List<Notification> getUserNotifications(Long userId, Boolean isRead, Integer page, Integer limit);

    /**
     * 统计未读通知数量
     *
     * @param userId 用户 ID
     * @return 未读数量
     */
    long countUnread(Long userId);

    /**
     * 批量标记为已读
     *
     * @param userId 用户 ID
     * @param notificationIds 通知 ID 列表
     */
    void markAsRead(Long userId, List<Long> notificationIds);

    /**
     * 清空所有通知
     *
     * @param userId 用户 ID
     */
    void clearAll(Long userId);
}
