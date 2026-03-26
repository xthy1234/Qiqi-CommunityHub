import http from '@/utils/http'
import type {
  Notification,
  NotificationListParams,
  NotificationListResponse,
  UnreadCountResponse,
} from '@/types/notification'

/**
 * 通知 API 类
 */
export class NotificationAPI {
  private endpoint = '/notifications'

  /**
   * 获取通知列表
   */
  async getList(params?: NotificationListParams) {
    const res = await http.get<NotificationListResponse>(this.endpoint, { params })
    return res.data
  }

  /**
   * 获取未读通知数量
   */
  async getUnreadCount() {
    const res = await http.get<UnreadCountResponse>(`${this.endpoint}/unread-count`)
    return res.data
  }

  /**
   * 批量标记为已读
   */
  async markRead(notificationIds: number[]) {
    const res = await http.put(`${this.endpoint}/mark-read`, { notificationIds })
    return res.data
  }

  /**
   * 一键全部已读
   */
  async markAllRead() {
    const res = await http.put(`${this.endpoint}/mark-all-read`)
    return res.data
  }

  /**
   * 清空通知
   */
  async clear() {
    const res = await http.delete(`${this.endpoint}/clear`)
    return res.data
  }

  /**
   * 删除单条通知
   */
  async delete(id: number) {
    const res = await http.delete(`${this.endpoint}/${id}`)
    return res.data
  }
}

// 导出单例
export const notificationAPI = new NotificationAPI()

// 兼容旧的导出方式
export const getNotificationList = (params?: NotificationListParams) => notificationAPI.getList(params)
export const getUnreadCount = () => notificationAPI.getUnreadCount()
export const markNotificationsRead = (notificationIds: number[]) => notificationAPI.markRead(notificationIds)
export const markAllRead = () => notificationAPI.markAllRead()
export const clearNotifications = () => notificationAPI.clear()
export const deleteNotification = (id: number) => notificationAPI.delete(id)
