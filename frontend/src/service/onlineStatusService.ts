import { getWebSocket } from '@/utils/websocket'
import { createStompSubscription } from '@/utils/websocketSubscription'
import { wsLogger } from '@/utils/websocketLogger'

/**
 * 用户在线状态数据
 */
export interface UserOnlineStatus {
  userId: number
  isOnline: boolean
  lastSeenAt?: string
  timestamp?: number
}

/**
 * 在线状态服务（纯业务逻辑，无 Vue 依赖）
 */
class OnlineStatusService {
  private statusHandlers: Set<(status: UserOnlineStatus) => void> = new Set()
  private listHandlers: Set<(users: Array<{ userId: number; online: boolean; lastSeenAt?: string }>) => void> = new Set()
  private unsubscribeStatus: (() => void) | null = null
  private unsubscribeList: (() => void) | null = null

  /**
   * 初始化在线状态订阅
   */
  init(userId: number): void {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      wsLogger.warn('WebSocket 未连接，无法初始化在线状态服务')
      return
    }

    const client = (ws as any).client
    if (!client) return

    wsLogger.info('初始化在线状态服务', { userId })

    // 订阅单个用户在线状态
    this.unsubscribeStatus = createStompSubscription(client, {
      messageType: 'USER_ONLINE_STATUS',
      destination: `/user/${userId}/queue/user-online-status`,
      handler: (data: UserOnlineStatus) => {
        wsLogger.debug('用户在线状态更新', data)
        this.statusHandlers.forEach(h => h(data))
      },
      logPrefix: 'OnlineStatusService'
    })

    // 订阅在线用户列表
    this.unsubscribeList = createStompSubscription(client, {
      messageType: 'USER_LIST_UPDATE',
      destination: '/topic/online-users',
      handler: (data: { users: Array<{ userId: number; online: boolean; lastSeenAt?: string }> }) => {
        wsLogger.debug('在线用户列表更新', { count: data.users?.length || 0 })
        this.listHandlers.forEach(h => h(data.users || []))
      },
      logPrefix: 'OnlineStatusService'
    })
  }

  /**
   * 监听用户在线状态
   */
  onStatusChange(handler: (status: UserOnlineStatus) => void): () => void {
    this.statusHandlers.add(handler)
    return () => this.statusHandlers.delete(handler)
  }

  /**
   * 监听用户列表
   */
  onListUpdate(handler: (users: Array<{ userId: number; online: boolean; lastSeenAt?: string }>) => void): () => void {
    this.listHandlers.add(handler)
    return () => this.listHandlers.delete(handler)
  }

  /**
   * 查询用户在线状态（主动请求）
   */
  queryUserStatus(userIds: number[]): void {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      wsLogger.warn('WebSocket 未连接，无法查询用户在线状态')
      return
    }

    wsLogger.debug('查询用户在线状态', { userIds })
    
    ;(ws as any).client.publish({
      destination: '/app/query-user-online-status',
      body: JSON.stringify({ userIds })
    })
  }

  /**
   * 订阅好友在线状态
   */
  subscribeFriendsStatus(): void {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      wsLogger.warn('WebSocket 未连接，无法订阅好友在线状态')
      return
    }

    wsLogger.debug('订阅好友在线状态')
    
    ;(ws as any).client.publish({
      destination: '/app/subscribe-friends-online-status',
      body: JSON.stringify({})
    })
  }

  /**
   * 清理订阅
   */
  destroy(): void {
    this.unsubscribeStatus?.()
    this.unsubscribeList?.()
    this.statusHandlers.clear()
    this.listHandlers.clear()
    
    wsLogger.info('在线状态服务已清理')
  }
}

export default new OnlineStatusService()
