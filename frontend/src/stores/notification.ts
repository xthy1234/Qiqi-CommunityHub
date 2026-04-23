import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  notificationAPI
} from '@/api/notification'
import type { Notification, NotificationListParams } from '@/types/notification'
import { getWebSocket } from '@/utils/websocket'
import { wsLogger } from '@/utils/websocketLogger'

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
  const isSubscribed = ref(false)

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
      const count = res?.data?.count || 0
      unreadCount.value = count

      
      return count
    } catch (error) {
      console.error('[NotificationStore] 加载未读数量失败:', error)
      throw error
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
      console.error('[NotificationStore] 标记已读失败:', error)
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
      console.error('[NotificationStore] 全部已读失败:', error)
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
      console.error('[NotificationStore] 清空通知失败:', error)
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
      console.error('[NotificationStore] 删除通知失败:', error)
      throw error
    }
  }

  /**
   * 添加新通知（WebSocket 推送）
   */
  function addNotification(notification: Notification) {

    
    // 处理 content 字段：如果是字符串，尝试解析为 JSON 对象
    const processedNotification = { ...notification }
    
    if (notification.content && typeof notification.content === 'string') {
      try {
        const parsed = JSON.parse(notification.content)
        processedNotification.content = parsed
      } catch (error) {
        console.warn('[NotificationStore] Content parse failed:', error)
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
    
    if (hasChanges || notificationIds.length > 0) {
      loadUnreadCount()

    }
  }

  /**
   * 清空所有通知（WebSocket 同步）
   */
  function clearAllNotifications() {

    
    notificationList.value = []
    unreadCount.value = 0

  }

  /**
   * 初始化 WebSocket 订阅（新版 - 使用 WebSocketManager）
   */
  function initWebSocketSubscription() {

    
    // 防止重复订阅
    if (isSubscribed.value) {
      console.warn('[NotificationStore] 已经订阅过通知，跳过重复订阅')
      return
    }

    const ws = getWebSocket()
    
    if (!ws) {
      console.error('[NotificationStore] WebSocket 实例不存在，无法订阅通知')
      return
    }

    if (!ws.isConnected()) {
      console.warn('[NotificationStore] WebSocket 未连接，等待连接后再订阅')
      // 监听连接状态，连接成功后自动订阅
      const unsubscribe = ws.onStateChange((state) => {
        if (state === 1) { // OPEN

          setupNotificationSubscriptions(ws)
          unsubscribe()
        }
      })
      return
    }

    setupNotificationSubscriptions(ws)
  }

  /**
   * 设置通知订阅
   */
  function setupNotificationSubscriptions(ws: any) {
    try {

      
      // 订阅新通知
      const unsubscribeNotification = ws.on('NOTIFICATION', (message: any) => {

        
        // 后端发送的格式：{ type: 'NOTIFICATION', data: {...} }
        // 需要提取 data 字段
        const notificationData = message.data || message

        
        addNotification(notificationData)
      })

      
      // 订阅已读状态更新
      const unsubscribeReadUpdate = ws.on('NOTIFICATION_READ_UPDATE', (message: any) => {

        
        // 提取 data 字段
        const data = message.data || message

        
        if (data.notificationIds) {
          updateReadStatus(data.notificationIds)
        }
      })

      // 订阅清空通知
      const unsubscribeClear = ws.on('NOTIFICATION_CLEAR', (message: any) => {

        
        clearAllNotifications()
      })

      isSubscribed.value = true


      // 返回取消订阅函数（可选）
      return () => {
        unsubscribeNotification()
        unsubscribeReadUpdate()
        unsubscribeClear()
        isSubscribed.value = false

      }
    } catch (error) {
      console.error('[NotificationStore] 设置通知订阅失败:', error)
      throw error
    }
  }

  /**
   * 重置订阅状态（用于登出等场景）
   */
  function resetSubscription() {
    isSubscribed.value = false

  }

  return {
    // State
    notificationList,
    unreadCount,
    isLoading,
    isSubscribed,
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
    resetSubscription
  }
})
