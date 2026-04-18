import toolUtil from './toolUtil'
import { Client, IMessage } from '@stomp/stompjs'
import type { IFrame } from '@stomp/stompjs'
import { createStompSubscription } from './websocketSubscription'
import { wsLogger } from './websocketLogger'
import type { 
  Message,
  NotificationPayload,
  UserOnlineStatusPayload,
  UserListUpdatePayload,
  MessageStatusPayload,
  MessageRecallPayload,
  MessageDeletePayload,
  NewConversationPayload,
  UnreadCountUpdatePayload
} from '@/types/message'
import type { 
  CircleMessage,
  CircleMessageDeletePayload 
} from '@/types/circleChat'

/**
 * WebSocket 消息类型枚举
 */
export type WsMessageType = 
  | 'CHAT_MESSAGE'
  | 'MESSAGE_STATUS'
  | 'MESSAGE_RECALL'
  | 'MESSAGE_DELETE'
  | 'USER_ONLINE_STATUS'
  | 'USER_LIST_UPDATE'
  | 'CIRCLE_CHAT_MESSAGE'
  | 'CIRCLE_CHAT_MESSAGE_DELETE'
  | 'NOTIFICATION'
  | 'NOTIFICATION_READ_UPDATE'
  | 'NEW_CONVERSATION'
  | 'UNREAD_COUNT_UPDATE'


/**
 * WebSocket 消息类型映射表
 */
export interface WsMessageMap {
  'CHAT_MESSAGE': Message
  'MESSAGE_STATUS': MessageStatusPayload['data']
  'MESSAGE_RECALL': MessageRecallPayload['data']
  'MESSAGE_DELETE': MessageDeletePayload['data']
  'USER_ONLINE_STATUS': UserOnlineStatusPayload['data']
  'USER_LIST_UPDATE': UserListUpdatePayload['data']
  'CIRCLE_CHAT_MESSAGE': CircleMessage
  'CIRCLE_CHAT_MESSAGE_DELETE': CircleMessageDeletePayload['data']
  'NOTIFICATION': NotificationPayload['data']
  'NOTIFICATION_READ_UPDATE': NotificationPayload['data']
  'NEW_CONVERSATION': NewConversationPayload['data']
  'UNREAD_COUNT_UPDATE': UnreadCountUpdatePayload['data']
}

/**
 * WebSocket 消息处理器类型
 */
export type WsMessageHandler<T extends WsMessageType> = (data: WsMessageMap[T]) => void

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
   * 获取当前用户 ID（简化版）
   * 统一从 UserInfo 中获取，确保数据一致性
   */
  private getCurrentUserId(): number | null {
    try {
      // 优先使用缓存的用户 ID（避免重复解析 JSON）
      if (this.currentUserId !== null && this.currentUserId > 0) {
        return this.currentUserId
      }
      
      // 从 UserInfo 中获取（唯一可信来源）
      const userInfoStr = toolUtil.storageGet('UserInfo')
      if (!userInfoStr) {
        wsLogger.warn('未找到 UserInfo，请检查是否已登录')
        return null
      }
      
      const userInfo = JSON.parse(userInfoStr)
      const userId = userInfo?.id
      
      // 验证 userId 有效性
      if (!userId || typeof userId !== 'number' || userId <= 0) {
        wsLogger.error('UserInfo 中的 userId 无效', { userId })
        return null
      }
      
      // 缓存用户 ID
      this.currentUserId = userId
      this.lastKnownUserId = userId
      
      wsLogger.debug('成功获取用户 ID', { userId })
      return userId
      
    } catch (error) {
      wsLogger.error('解析 UserInfo 失败', { 
        error: error instanceof Error ? error.message : error 
      })
      return null
    }
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
        console.warn(' [WebSocket] 用户 ID 变更，正在重新建立连接...')
        this.close()
        setTimeout(() => {
          this.connect().catch((error) => {
            console.error('[WebSocket] 重连失败:', error)
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
      console.error('[WebSocket] 用户 ID 一致性检查失败：未获取到用户 ID')
      return false
    }
    
    if (this.currentUserId && this.currentUserId !== currentUserId) {
      console.warn(' [WebSocket] 检测到用户 ID 不一致，缓存:', this.currentUserId, '实际:', currentUserId)
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
        

        const wsUrl = `${this.config.url}?userId=${userId}`

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
          

          connectHeaders: {
            Authorization: `Bearer ${token}`,  // 标准认证头（推荐）
            token: token,                      // 兼容当前后端拦截器
            userId: String(userId)             // 用户 ID（用于双重验证）
          },
          
          debug: (str) => {
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
            console.error('[WebSocket 调试] STOMP 错误:')
            console.error('  - Command:', frame.command)
            console.error('  - Headers:', JSON.stringify(frame.headers, null, 2))
            console.error('  - Message:', frame.headers?.message)
            console.error('  - Body:', frame.body)
            
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
            console.error('[WebSocket 调试] WebSocket 错误:', error)
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

        // 输出实际发送的 Connect Headers

        const connectHeaders = {
          Authorization: `Bearer ${token}`,
          token: token,
          userId: String(userId)
        }

        this.client.activate()
      } catch (error) {
        console.error('[WebSocket 调试] 连接异常:', error)
        wsLogger.error('连接异常', { error })
        reject(error)
      }
    })
  }

  /**
   * 连接成功后订阅消息队列
   */
  private subscribeToMessages(): void {
    if (!this.client) {return}

    const currentUserId = this.getCurrentUserId()
    
    if (!currentUserId) {
      wsLogger.error('无法订阅消息：未获取到有效的用户 ID')
      return
    }
  
    wsLogger.info('开始订阅消息队列', { userId: currentUserId })

    // 订阅所有必要的消息通道
    this.createMessageSubscriptions(currentUserId)
  }

  /**
   * 创建所有消息订阅
   */
  private createMessageSubscriptions(userId: number): void {
    if (!this.client) {return}

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
        description: '在线状态查询结果',
        handler: (data: any) => this.dispatchMessage('USER_ONLINE_STATUS', data)
      },
      {
        messageType: 'ONLINE_STATUS_CHANGE',
        destination: `/user/${userId}/queue/online-status-changes`,
        description: '在线状态变更通知',
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
    if (!this.client) {return}

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
      console.warn(' [WebSocket] 未连接，无法查询用户在线状态')
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
      console.warn(' [WebSocket] 未连接，无法订阅好友在线状态')
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
   * 发送已读回执（私聊）
   * @param messageSenderUserId 消息发送者的 ID
   */
  sendReadReceipt(messageSenderUserId: number): void {
    if (!this.client || !this.client.connected) {
      wsLogger.warn('WebSocket 未连接，无法发送已读回执')
      return
    }
    
    const currentUserId = this.getCurrentUserId()
    
    if (!currentUserId) {
      wsLogger.error('无法发送已读回执：未获取到当前用户 ID')
      return
    }
    
    // 修复：使用后端期望的字段名
    const receipt = {
      fromUserId: currentUserId,        // 当前用户（阅读者）
      toUserId: messageSenderUserId     // 消息发送方
      // lastReadMessageId: xxx         // 可选：最后一条已读消息 ID
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
      console.warn(' [WebSocket] 未连接，消息已跳过:', message)
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
   * 注册消息处理器（类型安全版本）
   * @param type 消息类型
   * @param handler 消息处理器，自动推断参数类型
   */
  on<T extends WsMessageType>(
    type: T, 
    handler: WsMessageHandler<T>
  ): () => void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set())
    }
    
    this.messageHandlers.get(type)!.add(handler as any)
    
    // 返回取消订阅函数
    return () => {
      this.offMessage(type, handler as any)
    }
  }

  /**
   * 移除消息处理器
   */
  offMessage<T extends WsMessageType>(
    type: T, 
    handler: WsMessageHandler<T>
  ): void {
    const handlers = this.messageHandlers.get(type)
    if (handlers) {
      handlers.delete(handler as any)
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
  close(): Promise<void> {
    return new Promise((resolve) => {
      if (this.client) {
        this.isManualClose = true
        
        // 检查客户端状态，避免在连接未完成时关闭
        const clientState = (this.client as any).state
        
        // 如果正在连接中，等待连接完成再关闭
        if (clientState === 'CONNECTING') {
          wsLogger.info('WebSocket 正在连接中，等待连接完成后关闭...')
          
          const checkConnection = setInterval(() => {
            const currentState = (this.client as any).state
            if (currentState === 'CONNECTED' || currentState === 'CLOSED' || currentState === 'INACTIVE') {
              clearInterval(checkConnection)
              
              if (currentState === 'CONNECTED') {
                // 已连接，正常关闭
                this.performClose(resolve)
              } else {
                // 已关闭或非活动状态，直接清理
                this.client = null
                wsLogger.info('WebSocket 已关闭（未连接状态）')
                resolve()
              }
            }
          }, 50)
          
          // 设置超时，防止无限等待
          setTimeout(() => {
            clearInterval(checkConnection)
            if (this.client) {
              try {
                this.client.deactivate()
              } catch (e) {
                // 忽略关闭错误
              }
              this.client = null
            }
            wsLogger.info('WebSocket 已强制关闭')
            resolve()
          }, 3000)
        } else if (clientState === 'CONNECTED') {
          // 已连接，正常关闭
          this.performClose(resolve)
        } else {
          // 其他状态（DEACTIVATING, INACTIVE, CLOSED），直接清理
          try {
            this.client.deactivate()
          } catch (e) {
            // 忽略关闭错误
          }
          this.client = null
          wsLogger.info('WebSocket 已关闭（非活动状态）')
          resolve()
        }
      } else {
        // 没有客户端，直接 resolve
        resolve()
      }
    })
  }

  /**
   * 执行关闭操作
   */
  private performClose(resolve: () => void): void {
    if (!this.client) {
      resolve()
      return
    }

    // 监听断开事件，等待 deactivate 完成
    const originalOnDisconnect = this.client.onDisconnect
    
    this.client.onDisconnect = (frame) => {
      // 调用原始的 onDisconnect（如果存在）
      if (originalOnDisconnect) {
        originalOnDisconnect(frame)
      }
      
      this.client = null
      wsLogger.info('WebSocket 已关闭')
      resolve()
    }
    
    // 触发关闭
    this.client.deactivate()
  }

  /**
   * 调度重连
   */
  private scheduleReconnect(): void {
    this.clearReconnectTimer()
    this.reconnectAttempts++
    
    this.reconnectTimer = setTimeout(() => {
      this.connect().catch((error) => {
        console.error('[WebSocket] 重连失败:', error)
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
        console.error('[WebSocket] 状态监听器执行出错:', error)
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
    console.warn(' [WebSocket] WebSocket 实例不存在，正在创建...')
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
