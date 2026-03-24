package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知 DAO 接口
 */
@Mapper
public interface NotificationDao extends BaseMapper<Notification> {

    /**
     * 查询用户的通知列表（分页）
     *
     * @param userId 用户 ID
     * @param isRead 是否已读（null 表示全部）
     * @return 通知列表
     */
    List<Notification> selectByUserId(@Param("userId") Long userId, 
                                      @Param("isRead") Boolean isRead);

    /**
     * 统计用户未读通知数量
     *
     * @param userId 用户 ID
     * @return 未读数量
     */
    Integer countUnread(@Param("userId") Long userId);

    /**
     * 批量标记为已读
     *
     * @param userId 用户 ID
     * @param notificationIds 通知 ID 列表
     * @return 影响行数
     */
    int markAsRead(@Param("userId") Long userId, 
                   @Param("notificationIds") List<Long> notificationIds);

    /**
     * 清空用户的所有通知
     *
     * @param userId 用户 ID
     * @return 影响行数
     */
    int clearAll(@Param("userId") Long userId);
}
