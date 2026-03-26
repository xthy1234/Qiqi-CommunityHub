import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  notificationAPI,
} from '@/api/notification'
import type { Notification, NotificationListParams } from '@/types/notification'

interface WebSocketClient {
  subscribe: (destination: string, callback: (message: any) => void) => void
  publish: (config: { destination: string; body: string }) => void
}

export const useNotificationStore = defineStore('notification', () => {
  // State
  const notificationList = ref<Notification[]>([])
  const unreadCount = ref<number>(0)
  const isLoading = ref(false)
  const wsClient = ref<WebSocketClient | null>(null)

  // Getters
  const hasUnread = computed(() => unreadCount.value > 0)

  // Actions
  /**
   * 加载通知列表
   */
  async function loadNotifications(params: NotificationListParams = {}) {
    isLoading.value = true
    try {
      const res = await notificationAPI.getList(params)
      notificationList.value = res.data?.list || []
      return res
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 加载未读数量
   */
  async function loadUnreadCount() {
    try {
      const res = await notificationAPI.getUnreadCount()
      unreadCount.value = res?.data?.count || 0
    } catch (error) {
      console.error('Failed to load unread count:', error)
    }
  }

  /**
   * 标记为已读
   */
  async function markAsRead(notificationIds: number[]) {
    try {
      await notificationAPI.markRead(notificationIds)
      // 更新本地状态
      notificationList.value.forEach((notification) => {
        if (notificationIds.includes(notification.id)) {
          notification.isRead = true
        }
      })
      // 重新计算未读数
      await loadUnreadCount()
    } catch (error) {
      console.error('Failed to mark notifications as read:', error)
      throw error
    }
  }

  /**
   * 全部已读
   */
  async function markAllAsRead() {
    try {
      await notificationAPI.markAllRead()
      // 更新本地状态
      notificationList.value.forEach((notification) => {
        notification.isRead = true
      })
      unreadCount.value = 0
    } catch (error) {
      console.error('Failed to mark all as read:', error)
      throw error
    }
  }

  /**
   * 清空通知
   */
  async function clearAll() {
    try {
      await notificationAPI.clear()
      notificationList.value = []
      unreadCount.value = 0
    } catch (error) {
      console.error('Failed to clear notifications:', error)
      throw error
    }
  }

  /**
   * 删除单条通知
   */
  async function removeNotification(id: number) {
    try {
      await notificationAPI.delete(id)
      const index = notificationList.value.findIndex((n) => n.id === id)
      if (index !== -1) {
        const wasUnread = !notificationList.value[index].isRead
        notificationList.value.splice(index, 1)
        if (wasUnread) {
          await loadUnreadCount()
        }
      }
    } catch (error) {
      console.error('Failed to delete notification:', error)
      throw error
    }
  }

  /**
   * 添加新通知（WebSocket 推送）
   */
  function addNotification(notification: Notification) {
    // 处理 content 字段：如果是字符串，尝试解析为 JSON 对象
    let processedNotification = { ...notification }
    
    if (notification.content && typeof notification.content === 'string') {
      try {
        const parsed = JSON.parse(notification.content)
        processedNotification.content = parsed
      } catch (error) {
        console.warn('⚠️ [NotificationStore] Content parse failed:', error)
        // 解析失败，保持原样
      }
    }
    
    notificationList.value.unshift(processedNotification)
    if (!processedNotification.isRead) {
      unreadCount.value++
    }
  }

  /**
   * 更新通知已读状态（WebSocket 同步）
   */
  function updateReadStatus(notificationIds: number[]) {
    let hasChanges = false
    notificationList.value.forEach((notification) => {
      if (notificationIds.includes(notification.id) && !notification.isRead) {
        notification.isRead = true
        hasChanges = true
      }
    })
    if (hasChanges) {
      loadUnreadCount()
    }
  }

  /**
   * 初始化 WebSocket 订阅
   */
  function initWebSocketSubscription(client: WebSocketClient) {
    wsClient.value = client
    const userId = localStorage.getItem('userId')
    if (!userId) {
      console.warn('No userId found, skipping notification subscription')
      return
    }

    // 订阅通知队列
    client.subscribe(`/user/${userId}/queue/notification`, (message: any) => {
      const data = JSON.parse(message.body)
      if (data.type === 'NOTIFICATION') {
        // 收到新通知
        addNotification(data.data)
      } else if (data.type === 'NOTIFICATION_READ_UPDATE') {
        // 收到已读状态更新
        updateReadStatus(data.data.notificationIds)
      }
    })
  }

  return {
    // State
    notificationList,
    unreadCount,
    isLoading,
    // Getters
    hasUnread,
    // Actions
    loadNotifications,
    loadUnreadCount,
    markAsRead,
    markAllAsRead,
    clearAll,
    removeNotification,
    addNotification,
    updateReadStatus,
    initWebSocketSubscription,
  }
})
