import toolUtil from './toolUtil'
import { Client, IMessage } from '@stomp/stompjs'
import type { IFrame } from '@stomp/stompjs'
import { createStompSubscription } from './websocketSubscription'
import { wsLogger } from './websocketLogger'

/**
 * WebSocket连接状态
 */
export enum WsReadyState {
  CONNECTING = 0,
  OPEN = 1,
  CLOSING = 2,
  CLOSED = 3
}

/**
 * WebSocket消息类型
 */
export interface WsMessage {
  type: string
  data: any
  timestamp?: number
}

/**
 * 私聊消息DTO（直接发送到后端）
 */
export interface PrivateMessageDTO {
  fromUserId: number
  toUserId: number
  content: string
  msgType?: number // 0-文本，1-图片，2-文件
}

/**
 * WebSocket配置接口
 */
export interface WsConfig {
  /** WebSocket服务器地址 */
  url: string
  /** 心跳间隔（毫秒），默认30000 */
  heartbeatInterval?: number
  /** 重连间隔（毫秒），默认5000 */
  reconnectInterval?: number
  /** 最大重连次数，默认5 */
  maxReconnectAttempts?: number
  /** 是否自动重连，默认true */
  autoReconnect?: boolean
}

/**
 * WebSocket 管理类（基于 STOMP）
 * 提供单例模式，全局唯一 WebSocket连接
 */
class WebSocketManager {
  private client: Client | null = null
  private config: WsConfig
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private messageHandlers: Map<string, Set<(data: any) => void>> = new Map()
  private stateListeners: Set<(state: WsReadyState) => void> = new Set()
  private isManualClose = false
  private currentUserId: number | null = null
  private lastKnownUserId: number | null = null // 新增：记录最后一次有效的用户 ID

  constructor(config: WsConfig) {
    this.config = {
      url: config.url,
      heartbeatInterval: config.heartbeatInterval || 30000,
      reconnectInterval: config.reconnectInterval || 5000,
      maxReconnectAttempts: config.maxReconnectAttempts || 5,
      autoReconnect: config.autoReconnect !== false
    }
  }

  /**
   * 获取全局 $message 实例
   */
  private getMessageInstance(): any {
    try {
      const win = window as any
      const app = win.__vue_app__
      if (app?._instance?.config?.globalProperties?.$message) {
        return app._instance.config.globalProperties.$message
      }
    } catch (error) {
      // 静默失败
    }
    return null
  }

  /**
   * 显示消息提示
   */
  private showMessage(type: 'success' | 'error' | 'warning' | 'info', content: string): void {
    const $message = this.getMessageInstance()
    if ($message && typeof $message[type] === 'function') {
      $message[type](content)
    } else {
      console.error(`[WebSocket ${type.toUpperCase()}]`, content)
    }
  }

  /**
   * 获取当前用户 ID（优化版：修复竞态条件和用户切换问题）
   */
  private getCurrentUserId(): number | null {
    // 1. 优先使用缓存的用户 ID（但不为 0）
    if (this.currentUserId !== null && this.currentUserId !== undefined && this.currentUserId > 0) {
      return this.currentUserId
    }
    
    // 2. 尝试从 UserInfo 中获取（推荐方式）
    const userInfoStr = toolUtil.storageGet('UserInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        const userId = userInfo?.id
        
        // 验证 userId 有效性
        if (userId !== undefined && userId !== null && userId > 0) {
          this.currentUserId = userId
          this.lastKnownUserId = userId
          return userId
        }
      } catch (error) {
        console.error('❌ [WebSocket] 解析 UserInfo 失败:', error)
      }
    }
    
    // 3. 降级方案：尝试从 userid 获取（兼容旧数据）
    const userIdStr = toolUtil.storageGet('userid')
    if (userIdStr) {
      const userId = parseInt(userIdStr, 10)
      
      // 验证 userId 有效性
      if (!isNaN(userId) && userId > 0) {
        this.currentUserId = userId
        this.lastKnownUserId = userId
        return userId
      }
    }
    
    // 4. 如果都失败了，返回 lastKnownUserId（用于诊断）
    if (this.lastKnownUserId) {
      console.warn('⚠️ [WebSocket] 无法获取当前用户 ID，使用上次已知 ID:', this.lastKnownUserId)
      return this.lastKnownUserId
    }
    
    console.warn('⚠️ [WebSocket] 未获取到有效的用户 ID')
    return null
  }

  /**
   * 重置用户 ID（用于用户登出场景）
   */
  public resetUserId(): void {

    this.currentUserId = null
    this.lastKnownUserId = null
  }

  /**
   * 更新用户 ID（用于用户切换场景）
   */
  public updateUserId(userId: number): void {
    if (userId !== this.currentUserId) {

      this.currentUserId = userId
      this.lastKnownUserId = userId
      
      // 如果已连接，需要重新建立连接（因为 URL 中包含 userId）
      if (this.client && this.client.connected) {
        console.warn('⚠️ [WebSocket] 用户 ID 变更，正在重新建立连接...')
        this.close()
        setTimeout(() => {
          this.connect().catch((error) => {
            console.error('❌ [WebSocket] 重连失败:', error)
          })
        }, 1000)
      }
    }
  }

  /**
   * 检查用户 ID 是否发生变化（用于 Token 刷新时同步检查）
   */
  public checkUserIdConsistency(): boolean {
    const currentUserId = this.getCurrentUserId()
    
    if (!currentUserId) {
      console.error('❌ [WebSocket] 用户 ID 一致性检查失败：未获取到用户 ID')
      return false
    }
    
    if (this.currentUserId && this.currentUserId !== currentUserId) {
      console.warn('⚠️ [WebSocket] 检测到用户 ID 不一致，缓存:', this.currentUserId, '实际:', currentUserId)
      this.currentUserId = currentUserId
      this.lastKnownUserId = currentUserId
      return false // 不一致，需要重新连接
    }
    
    return true // 一致
  }

  /**
   * 连接 WebSocket（使用 STOMP）- 按照后端文档实现
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        const token = toolUtil.storageGet('Token')
        const userId = this.getCurrentUserId()
        
        // 增强错误提示
        if (!userId) {
          const errorMsg = '未获取到用户 ID，无法建立 WebSocket 连接'
          wsLogger.error(errorMsg, {
            hint1: '请检查是否已登录',
            hint2: '请检查 storage 中是否存在 UserInfo 或 userid'
          })
          throw new Error(errorMsg)
        }
        
        if (!token) {
          const errorMsg = '未获取到 Token，无法建立 WebSocket 连接'
          wsLogger.error(errorMsg, {
            hint1: '请检查是否已登录',
            hint2: '请检查 storage 中是否存在 Token'
          })
          throw new Error(errorMsg)
        }
        
        // 关键：按照文档构建完整的 URL
        const wsUrl = `${this.config.url}?userId=${userId}&token=${encodeURIComponent(token)}`
        
        // 如果已有连接，先关闭
        if (this.client) {
          wsLogger.warn('检测到已有连接，正在关闭...')
          this.close()
        }

        this.isManualClose = false
        
        wsLogger.logConnectionState('CONNECTING')
        
        // 创建 STOMP 客户端
        this.client = new Client({
          brokerURL: wsUrl,
          reconnectDelay: this.config.reconnectInterval!,
          heartbeatIncoming: this.config.heartbeatInterval! / 2,
          heartbeatOutgoing: this.config.heartbeatInterval! / 2,
          
          // 关键：在 STOMP CONNECT 帧的 headers 中添加认证信息
          connectHeaders: {
            Authorization: `Bearer ${token}`,  // 标准认证头（推荐）
            token: token,                      // 兼容当前后端拦截器
            userId: String(userId)             // 用户 ID（用于双重验证）
          },
          
          debug: (str) => {
            // 生产环境可关闭调试日志
            wsLogger.debug('STOMP Debug', { message: str })
          },
          
          onConnect: () => {
            this.reconnectAttempts = 0
            this.notifyStateChange(WsReadyState.OPEN)
            this.subscribeToMessages()
            wsLogger.logConnectionState('CONNECTED')
            resolve()
          },
          
          onStompError: (frame: IFrame) => {
            wsLogger.error('STOMP 错误', {
              command: frame.command,
              headers: frame.headers,
              message: frame.headers?.message,
              body: frame.body
            })
            
            this.notifyStateChange(WsReadyState.CLOSED)
            reject(new Error(frame.headers?.message || 'STOMP error'))
          },
          
          onWebSocketError: (error) => {
            wsLogger.error('WebSocket 错误', { error })
            this.notifyStateChange(WsReadyState.CLOSED)
            reject(error)
          },
          
          onDisconnect: () => {
            this.notifyStateChange(WsReadyState.CLOSED)
            wsLogger.logConnectionState('DISCONNECTED')
            
            if (this.isManualClose) {
              return
            }
            
            if (this.config.autoReconnect && this.reconnectAttempts < this.config.maxReconnectAttempts!) {
              wsLogger.logConnectionState('RECONNECTING')
              this.scheduleReconnect()
            } else if (this.reconnectAttempts >= this.config.maxReconnectAttempts!) {
              wsLogger.error('已达到最大重连次数', { attempts: this.reconnectAttempts })
              this.showMessage('error', 'WebSocket 连接失败，已超出最大重试次数')
            }
          }
        })

        this.client.activate()
      } catch (error) {
        wsLogger.error('连接异常', { error })
        reject(error)
      }
    })
  }

  /**
   * 连接成功后订阅消息队列（重构版）
   */
  private subscribeToMessages(): void {
    if (!this.client) {return}

    const currentUserId = this.getCurrentUserId()
    
    // 关键修复：检查 userId 有效性
    if (!currentUserId) {
      wsLogger.error('无法订阅消息：未获取到有效的用户 ID')
      return
    }
  
    wsLogger.info('开始订阅消息队列', { userId: currentUserId })

    // 使用标准化工具创建订阅，消除重复代码
    this.createMessageSubscriptions(currentUserId)
  }

  /**
   * 创建所有消息订阅（核心优化：抽取公共逻辑）
   */
  private createMessageSubscriptions(userId: number): void {
    if (!this.client) return

    // 定义所有订阅配置
    const subscriptions = [
      {
        messageType: 'CHAT_MESSAGE',
        destination: `/user/${userId}/queue/private-messages`,
        description: '私聊消息',
        handler: (data: any) => this.dispatchMessage('CHAT_MESSAGE', data)
      },
      {
        messageType: 'MESSAGE_STATUS',
        destination: `/user/${userId}/queue/read-receipts`,
        description: '已读回执',
        handler: (data: any) => this.dispatchMessage('MESSAGE_STATUS', data)
      },
      {
        messageType: 'MESSAGE_RECALL',
        destination: `/user/${userId}/queue/message-recall`,
        description: '撤回通知',
        handler: (data: any) => this.dispatchMessage('MESSAGE_RECALL', data)
      },
      {
        messageType: 'MESSAGE_DELETE',
        destination: `/user/${userId}/queue/message-delete`,
        description: '删除通知',
        handler: (data: any) => this.dispatchMessage('MESSAGE_DELETE', data)
      },
      {
        messageType: 'USER_ONLINE_STATUS',
        destination: `/user/${userId}/queue/user-online-status`,
        description: '在线状态',
        handler: (data: any) => this.dispatchMessage('USER_ONLINE_STATUS', data)
      },
      {
        messageType: 'CIRCLE_CHAT_MESSAGE',
        destination: `/topic/circles/*/messages`,
        description: '圈子消息',
        handler: (data: any) => this.dispatchMessage('CIRCLE_CHAT_MESSAGE', data)
      },
      {
        messageType: 'NOTIFICATION',
        destination: `/user/${userId}/queue/notification`,
        description: '系统通知',
        handler: (data: any) => this.dispatchMessage('NOTIFICATION', data)
      },
      {
        messageType: 'NOTIFICATION_READ_UPDATE',
        destination: `/user/${userId}/queue/notification`,
        description: '通知已读更新',
        handler: (data: any) => this.dispatchMessage('NOTIFICATION_READ_UPDATE', data)
      }
    ]

    // 批量创建订阅
    subscriptions.forEach(config => {
      createStompSubscription(this.client!, {
        messageType: config.messageType,
        destination: config.destination,
        handler: config.handler,
        logPrefix: config.description
      })
      
      wsLogger.logSubscription(config.destination, config.messageType)
    })

    // 单独处理用户列表订阅（通配符订阅）
    this.subscribeUserList()
  }

  /**
   * 分发消息到注册的处理器
   */
  private dispatchMessage(type: string, data: any): void {
    const handlers = this.messageHandlers.get(type)
    
    if (!handlers || handlers.size === 0) {
      wsLogger.warn(`未找到${type}处理器`, { 
        availableTypes: Array.from(this.messageHandlers.keys()) 
      })
      return
    }

    let successCount = 0
    handlers.forEach(handler => {
      try {
        handler(data)
        successCount++
      } catch (error) {
        wsLogger.error(`${type} 处理器执行失败`, { 
          error: error instanceof Error ? error.message : error 
        })
      }
    })

    wsLogger.debug(`${type} 消息分发完成`, { 
      totalHandlers: handlers.size, 
      successCount 
    })
  }

  /**
   * 订阅用户列表（批量推送）
   */
  private subscribeUserList(): void {
    if (!this.client) return

    createStompSubscription(this.client, {
      messageType: 'USER_LIST_UPDATE',
      destination: '/topic/online-users',
      handler: (data: any) => this.dispatchMessage('USER_LIST_UPDATE', data),
      logPrefix: '在线用户列表'
    })
  }

  /**
   * 请求获取指定用户的在线状态（主动查询）
   * @param userIds 用户ID数组
   */
  public queryUserOnlineStatus(userIds: number[]): void {
    if (!this.client || !this.client.connected) {
      console.warn('⚠️ [WebSocket] 未连接，无法查询用户在线状态')
      return
    }

    const request = {
      userIds: userIds
    }
    
    this.client.publish({
      destination: '/app/query-user-online-status',
      body: JSON.stringify(request)
    })
  }

  /**
   * 订阅所有好友的在线状态
   */
  public subscribeFriendsOnlineStatus(): void {
    if (!this.client || !this.client.connected) {
      console.warn('⚠️ [WebSocket] 未连接，无法订阅好友在线状态')
      return
    }
    
    this.client.publish({
      destination: '/app/subscribe-friends-online-status',
      body: JSON.stringify({})
    })
  }

  /**
   * 发送私聊消息（关键方法！）-按照后端文档实现
   */
  sendPrivateMessage(toUserId: number, chatMessage: any): void {
    if (!this.client || !this.client.connected) {
      wsLogger.warn('未连接，消息已跳过', { toUserId })
      return
    }
    
    const fromUserId = this.getCurrentUserId()
    if (!fromUserId) {
      wsLogger.error('无法发送消息：未获取到当前用户 ID')
      return
    }
    
    // 关键：content 应该是 JSON 对象，不要二次序列化
    const message: PrivateMessageDTO = {
      fromUserId,
      toUserId,
      content: chatMessage,
      msgType: chatMessage.msgType
    }

    wsLogger.debug('发送私聊消息', {
      fromUserId,
      toUserId,
      msgType: message.msgType,
      contentLength: typeof message.content === 'string' ? message.content.length : JSON.stringify(message.content).length
    })

    // 发送到后端端点
    this.client.publish({
      destination: '/app/private-message',
      body: JSON.stringify(message)
    })
  }

  /**
   * 发送已读回执
   */
  sendReadReceipt(fromUserId: number): void {
    if (!this.client || !this.client.connected) {
      wsLogger.warn('WebSocket 未连接，无法发送已读回执')
      return
    }
    
    const currentUserId = this.getCurrentUserId()
    
    if (!currentUserId) {
      wsLogger.error('无法发送已读回执：未获取到当前用户 ID')
      return
    }
    
    const receipt = {
      fromUserId: currentUserId,      // 我（阅读者）
      toUserId: fromUserId,           // 对方（发送者）
      timestamp: Date.now()
      // lastReadMessageId: 123       // 可选：最后一条已读消息 ID
    }
    
    try {
      this.client.publish({
        destination: '/app/read-receipt',
        body: JSON.stringify(receipt)
      })
      
      wsLogger.debug('已读回执已发送', receipt)
    } catch (error) {
      wsLogger.error('发送已读回执失败', { 
        error: error instanceof Error ? error.message : error,
        receipt 
      })
    }
  }

  /**
   * 撤回消息
   */
  recallMessage(messageId: number, reason = ''): void {
    
    if (!this.client || !this.client.connected) {
      wsLogger.warn('未连接，无法撤回消息')
      return
    }
    
    const currentUserId = this.getCurrentUserId()
    
    if (!currentUserId) {
      wsLogger.error('无法撤回消息：未获取到当前用户 ID')
      return
    }
    
    const request = {
      messageId: messageId,
      userId: currentUserId,
      reason: reason
    }
    
    try {
      this.client.publish({
        destination: '/app/recall-message',
        body: JSON.stringify(request)
      })
      
      wsLogger.debug('撤回请求已发送', request)
    } catch (error) {
      wsLogger.error('发送撤回请求失败', { 
        error: error instanceof Error ? error.message : error,
        requestId: messageId 
      })
    }
  }

  /**
   * 删除消息
   */
  deleteMessage(messageId: number): void {
    
    if (!this.client || !this.client.connected) {
      wsLogger.warn('未连接，无法删除消息')
      return
    }
    
    const currentUserId = this.getCurrentUserId()
    
    if (!currentUserId) {
      wsLogger.error('无法删除消息：未获取到当前用户 ID')
      return
    }
    
    const request = {
      messageId: messageId,
      userId: currentUserId
    }
    
    try {
      this.client.publish({
        destination: '/app/delete-message',
        body: JSON.stringify(request)
      })
      
      wsLogger.debug('删除请求已发送', request)
    } catch (error) {
      wsLogger.error('发送删除请求失败', { 
        error: error instanceof Error ? error.message : error,
        messageId 
      })
    }
  }

  /**
   * 发送消息（兼容旧接口）
   */
  send(message: WsMessage): void {
    if (!this.client || !this.client.connected) {
      console.warn('⚠️ [WebSocket] 未连接，消息已跳过:', message)
      return
    }
    
    // PING消息保持原有逻辑
    if (message.type === 'PING') {

      this.client.publish({
        destination: '/app/ping',
        body: JSON.stringify({ timestamp: Date.now() })
      })
    }
  }

  /**
   * 注册消息处理器（兼容旧接口）
   * @param type 消息类型：CHAT_MESSAGE | MESSAGE_STATUS | MESSAGE_RECALL | MESSAGE_DELETE | USER_ONLINE_STATUS | USER_LIST_UPDATE | CIRCLE_CHAT_MESSAGE | CIRCLE_CHAT_MESSAGE_DELETE | NOTIFICATION | NOTIFICATION_READ_UPDATE
   */
  on(type: 'CHAT_MESSAGE' | 'MESSAGE_STATUS' | 'MESSAGE_RECALL' | 'MESSAGE_DELETE' | 'USER_ONLINE_STATUS' | 'USER_LIST_UPDATE' | 'CIRCLE_CHAT_MESSAGE' | 'CIRCLE_CHAT_MESSAGE_DELETE' | 'NOTIFICATION' | 'NOTIFICATION_READ_UPDATE', handler: (data: any) => void): () => void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set())
    }
    
    this.messageHandlers.get(type)!.add(handler)
    
    // 返回取消订阅函数
    return () => {
      this.offMessage(type, handler)
    }
  }

  /**
   * 移除消息处理器
   */
  offMessage(type: 'CHAT_MESSAGE' | 'MESSAGE_STATUS' | 'MESSAGE_RECALL' | 'MESSAGE_DELETE' | 'USER_ONLINE_STATUS' | 'USER_LIST_UPDATE' | 'CIRCLE_CHAT_MESSAGE' | 'CIRCLE_CHAT_MESSAGE_DELETE' | 'NOTIFICATION' | 'NOTIFICATION_READ_UPDATE', handler: (data: any) => void): void {
    const handlers = this.messageHandlers.get(type)
    if (handlers) {
      handlers.delete(handler)

    }
  }

  /**
   * 注册连接状态监听器
   */
  onStateChange(listener: (state: WsReadyState) => void): () => void {
    this.stateListeners.add(listener)
    return () => {
      this.stateListeners.delete(listener)
    }
  }

  /**
   * 获取当前连接状态
   */
  getState(): WsReadyState {
    return this.client?.connected ? WsReadyState.OPEN : WsReadyState.CLOSED
  }

  /**
   * 是否已连接
   */
  isConnected(): boolean {
    return this.client?.connected || false
  }

  /**
   * 关闭 WebSocket连接
   */
  close(): void {
    if (this.client) {
      this.isManualClose = true
      this.client.deactivate()
      this.client = null
      // 注意：这里不清除 currentUserId，以便重连时使用
    }
  }

  /**
   * 调度重连
   */
  private scheduleReconnect(): void {
    this.clearReconnectTimer()
    this.reconnectAttempts++
    
    this.reconnectTimer = setTimeout(() => {
      this.connect().catch((error) => {
        console.error('❌ [WebSocket] 重连失败:', error)
      })
    }, this.config.reconnectInterval)
  }

  /**
   * 清除重连定时器
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 通知状态变化
   */
  private notifyStateChange(state: WsReadyState): void {
    this.stateListeners.forEach(listener => {
      try {
        listener(state)
      } catch (error) {
        console.error('❌ [WebSocket] 状态监听器执行出错:', error)
      }
    })
  }
}

// 创建全局单例
let wsManager: WebSocketManager | null = null

/**
 * 初始化 WebSocket
 * @param url WebSocket服务器地址，例如：ws://localhost:8080/ws
 */
export function initWebSocket(url?: string): WebSocketManager {
  if (wsManager) {
    return wsManager
  }

  const wsUrl = url || `ws://${window.location.hostname}:8080/ws`
  
  wsManager = new WebSocketManager({
    url: wsUrl,
    heartbeatInterval: 30000,
    reconnectInterval: 5000,
    maxReconnectAttempts: 5,
    autoReconnect: true
  })

  return wsManager
}

/**
 * 获取 WebSocket 实例
 */
export function getWebSocket(): WebSocketManager | null {
  return wsManager
}

/**
 * 检查并获取 WebSocket 实例（如果不存在则初始化）
 * @param url WebSocket服务器地址，可选
 */
export function getOrCreateWebSocket(url?: string): WebSocketManager {
  if (!wsManager) {
    console.warn('⚠️ [WebSocket] WebSocket 实例不存在，正在创建...')
    return initWebSocket(url)
  }
  return wsManager
}

/**
 * 确保 WebSocket 已连接
 * @returns Promise<void> 连接成功后 resolve
 */
export async function ensureConnected(url?: string): Promise<void> {
  const ws = getOrCreateWebSocket(url)
  
  if (!ws.isConnected()) {

    await ws.connect()

  } else {

  }
}

/**
 * 重置 WebSocket 用户 ID（用于用户登出）
 * 使用方法：logout() 之后调用
 *
* await logout()
* resetWebSocketUserId()
 **/

export function resetWebSocketUserId(): void {
  const ws = getWebSocket()
  if (ws) {
    ws.resetUserId()
  }
}

/**
 * 更新 WebSocket 用户 ID（用于用户切换）
 * 使用方法：用户切换成功后调用
 * await switchUser(newUserId)
 updateWebSocketUserId(newUserId)

 */
export function updateWebSocketUserId(userId: number): void {
  const ws = getWebSocket()
  if (ws) {
    ws.updateUserId(userId)
  }
}

export default WebSocketManager
