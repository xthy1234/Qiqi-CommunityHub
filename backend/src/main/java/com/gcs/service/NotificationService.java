package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Notification;
import com.gcs.utils.PageUtils;

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

    /**
     * 批量发送通知给多个用户
     *
     * @param userIds 用户 ID 列表（为空表示全员）
     * @param type 通知类型
     * @param content 通知内容
     * @param extra 额外数据
     * @return 发送的用户数量
     */
    int sendBatchNotifications(List<Long> userIds, Integer type, Map<String, Object> content, Map<String, Object> extra);

    /**
     * 发送系统广播（全员通知）
     *
     * @param type 通知类型
     * @param content 通知内容
     * @param extra 额外数据
     * @return 发送的用户数量
     */
    int sendBroadcastNotification(Integer type, Map<String, Object> content, Map<String, Object> extra);

    /**
     * 撤回/删除指定通知
     *
     * @param notificationId 通知 ID
     * @return 是否成功
     */
    boolean withdrawNotification(Long notificationId);

    /**
     * 批量撤回通知
     *
     * @param notificationIds 通知 ID 列表
     * @return 撤回的数量
     */
    int batchWithdrawNotifications(List<Long> notificationIds);

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 带条件的分页查询通知列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Notification> queryWrapper);
}
