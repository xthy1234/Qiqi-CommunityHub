import toolUtil from './toolUtil'
import { ElMessage } from 'element-plus'
import { Client, IMessage } from '@stomp/stompjs'

/**
 * WebSocket 连接状态
 */
export enum WsReadyState {
  CONNECTING = 0,
  OPEN = 1,
  CLOSING = 2,
  CLOSED = 3
}

/**
 * WebSocket 消息类型
 */
export interface WsMessage {
  type: string
  data: any
  timestamp?: number
}

/**
 * 私聊消息 DTO（直接发送到后端）
 */
export interface PrivateMessageDTO {
  fromUserId: number
  toUserId: number
  content: string
  msgType?: number // 0-文本，1-图片，2-文件
}

/**
 * WebSocket 配置接口
 */
export interface WsConfig {
  /** WebSocket 服务器地址 */
  url: string
  /** 心跳间隔（毫秒），默认 30000 */
  heartbeatInterval?: number
  /** 重连间隔（毫秒），默认 5000 */
  reconnectInterval?: number
  /** 最大重连次数，默认 5 */
  maxReconnectAttempts?: number
  /** 是否自动重连，默认 true */
  autoReconnect?: boolean
}

/**
 * WebSocket 管理类（基于 STOMP）
 * 提供单例模式，全局唯一 WebSocket 连接
 */
class WebSocketManager {
  private client: Client | null = null
  private config: WsConfig
  private reconnectTimer: any = null
  private reconnectAttempts = 0
  private messageHandlers: Map<string, Set<(data: any) => void>> = new Map()
  private stateListeners: Set<(state: WsReadyState) => void> = new Set()
  private isManualClose = false
  private currentUserId: number | null = null

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
   * 获取当前用户 ID
   */
  private getCurrentUserId(): number | null {
    if (this.currentUserId) {
      return this.currentUserId
    }
    
    const userInfoStr = toolUtil.storageGet('UserInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        this.currentUserId = userInfo?.id

        return this.currentUserId
      } catch (error) {
        console.error('❌ [WebSocket] 获取用户 ID 失败:', error)
      }
    }
    
    // 降级方案：尝试从 userid 读取
    const userIdStr = toolUtil.storageGet('userid')
    if (userIdStr) {
      this.currentUserId = parseInt(userIdStr)

      return this.currentUserId
    }
    
    console.warn('⚠️ [WebSocket] 未获取到用户 ID')
    return null
  }

  /**
   * 连接 WebSocket（使用 STOMP）- 按照后端文档实现
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        const token = toolUtil.storageGet('Token')
        const userId = this.getCurrentUserId()
        
        if (!userId) {
          throw new Error('未获取到用户 ID，无法建立 WebSocket 连接')
        }
        
        if (!token) {
          throw new Error('未获取到 Token，无法建立 WebSocket 连接')
        }
        
        //  关键：按照文档构建完整的 URL
        const wsUrl = `${this.config.url}?userId=${userId}&token=${encodeURIComponent(token)}`


        
        // 如果已有连接，先关闭
        if (this.client) {
          console.warn('⚠️ [WebSocket] 关闭已有连接')
          this.close()
        }

        this.isManualClose = false
        
        // 创建 STOMP 客户端
        this.client = new Client({
          brokerURL: wsUrl,
          reconnectDelay: this.config.reconnectInterval!,
          heartbeatIncoming: this.config.heartbeatInterval! / 2,
          heartbeatOutgoing: this.config.heartbeatInterval! / 2,
          
          //  关键：在 STOMP CONNECT 帧的 headers 中添加认证信息
          connectHeaders: {
            Authorization: `Bearer ${token}`,  // 标准认证头（推荐）
            token: token,                      // 兼容当前后端拦截器
            userId: String(userId)             // 用户 ID（用于双重验证）
          },
          
          debug: (str) => {

          },
          
          onConnect: () => {

            this.reconnectAttempts = 0
            this.notifyStateChange(WsReadyState.OPEN)
            this.subscribeToMessages()
            resolve()
          },
          
          onStompError: (frame) => {
            console.error('❌ [WebSocket] STOMP 错误:')
            console.error('  - Command:', frame.command)
            console.error('  - Headers:', frame.headers)
            console.error('  - Message:', frame.headers?.message)
            console.error('  - Body:', frame.body)
            
            this.notifyStateChange(WsReadyState.CLOSED)
            reject(new Error(frame.headers?.message || 'STOMP error'))
          },
          
          onWebSocketError: (error) => {
            console.error('❌ [WebSocket] WebSocket 错误:', error)
            this.notifyStateChange(WsReadyState.CLOSED)
            reject(error)
          },
          
          onDisconnect: () => {

            this.notifyStateChange(WsReadyState.CLOSED)
            
            if (this.isManualClose) {

              return
            }
            
            if (this.config.autoReconnect && this.reconnectAttempts < this.config.maxReconnectAttempts!) {
              this.scheduleReconnect()
            } else if (this.reconnectAttempts >= this.config.maxReconnectAttempts!) {
              console.error('❌ [WebSocket] 已达到最大重连次数:', this.reconnectAttempts)
              ElMessage.error('WebSocket 连接失败，已超出最大重试次数')
            }
          }
        })

        this.client.activate()
      } catch (error) {
        console.error('❌ [WebSocket] 连接异常:', error)
        reject(error)
      }
    })
  }

  /**
   * 连接成功后订阅各类消息队列
   */
  private subscribeToMessages(): void {
    if (!this.client) return

    const currentUserId = this.getCurrentUserId()

    // console.log('✅ [WebSocket] 开始订阅消息队列，当前用户 ID:', currentUserId)
    
    // 订阅私聊消息（使用明确路径）
    const privateMsgDestination = `/user/${currentUserId}/queue/private-messages`

    this.client.subscribe(privateMsgDestination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        const handlers = this.messageHandlers.get('CHAT_MESSAGE')

        if (handlers && handlers.size > 0) {
          let successCount = 0
          handlers.forEach(handler => {
            try {
              handler(data)
              successCount++
            } catch (error) {
              console.error(`❌ [WebSocket] 单个处理器执行出错:`, error)
            }
          })

          console.log('✅ [WebSocket] CHAT_MESSAGE 处理器执行完成:', 
              '- 成功数量:', successCount,
              '- 总数量:', handlers.size)
        } else {
          console.warn('⚠️ [WebSocket] 未找到 CHAT_MESSAGE 处理器！')
        }
      } catch (error) {
        console.error('❌ [WebSocket] 消息解析失败:', error)
        console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
        console.error('  - 错误信息:', error instanceof Error ? error.message : error)
        console.error('  - 原始数据:', message.body)
      }
    }, {})
    
    // console.log('✅ [WebSocket] 已订阅私聊消息队列:', privateMsgDestination)

    // 订阅已读回执队列
    const receiptDestination = `/user/${currentUserId}/queue/read-receipts`

    this.client.subscribe(receiptDestination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        const handlers = this.messageHandlers.get('MESSAGE_STATUS')

        if (handlers && handlers.size > 0) {
          let successCount = 0
          let index = 0
          handlers.forEach((handler: (data: any) => void) => {
            try {
              handler(data)
              successCount++
            } catch (error) {
              console.error(`❌ [WebSocket] 单个处理器执行出错:`, error)
            } finally {
              index++
            }
          })
          
          // console.log('✅ [WebSocket] MESSAGE_STATUS 处理器执行完成:',
          //     '- 成功数量:', successCount,
          //     '- 总数量:', handlers.size)
        } else {
          console.warn('⚠️ [WebSocket] 未找到 MESSAGE_STATUS 处理器！', 
              '- 当前所有处理器类型:', Array.from(this.messageHandlers.keys()))
        }
      } catch (error) {
        console.error('❌ [WebSocket] 已读回执解析失败:', error)
        console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
        console.error('  - 错误信息:', error instanceof Error ? error.message : error)
        console.error('  - 原始数据:', message.body)
      }
    }, {})
    
    // console.log('✅ [WebSocket] 已订阅已读回执队列:', receiptDestination)

    // 订阅撤回通知
    const recallDestination = `/user/${currentUserId}/queue/message-recall`

    this.client.subscribe(recallDestination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        const handlers = this.messageHandlers.get('MESSAGE_RECALL')

        if (handlers && handlers.size > 0) {
          let successCount = 0
          let index = 0
          handlers.forEach((handler: (data: any) => void) => {
            try {
              handler(data)
              successCount++
            } catch (error) {
              console.error(`❌ [WebSocket] 单个处理器执行出错:`, error)
            } finally {
              index++
            }
          })
          // console.log('✅ [WebSocket] MESSAGE_RECALL 处理器执行完成:',
          //     '- 成功数量:', successCount,
          //     '- 总数量:', handlers.size)
        } else {
          console.warn('⚠️ [WebSocket] 未找到 MESSAGE_RECALL 处理器！')
        }
      } catch (error) {
        console.error('❌ [WebSocket] 消息撤回通知解析失败:', error)
        console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
        console.error('  - 错误信息:', error instanceof Error ? error.message : error)
        console.error('  - 原始数据:', message.body)
      }
    }, {})
    
    // console.log('✅ [WebSocket] 已订阅消息撤回队列:', recallDestination)

    // 订阅消息删除通知
    const deleteDestination = `/user/${currentUserId}/queue/message-delete`

    this.client.subscribe(deleteDestination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        const handlers = this.messageHandlers.get('MESSAGE_DELETE')

        if (handlers && handlers.size > 0) {
          let successCount = 0
          handlers.forEach(handler => {
            try {
              handler(data)
              successCount++
            } catch (error) {
              console.error(`❌ [WebSocket] 单个处理器执行出错:`, error)
            }
          })

          // console.log('✅ [WebSocket] MESSAGE_DELETE 处理器执行完成:',
          //     '- 成功数量:', successCount,
          //     '- 总数量:', handlers.size)
        } else {
          console.warn('⚠️ [WebSocket] 未找到 MESSAGE_DELETE 处理器！')
        }
      } catch (error) {
        console.error('❌ [WebSocket] 消息删除通知解析失败:', error)
        console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
        console.error('  - 错误信息:', error instanceof Error ? error.message : error)
        console.error('  - 原始数据:', message.body)
      }
    }, {})
    
    // ====== 圈子消息删除通知已在 CIRCLE_CHAT_MESSAGE 中统一处理，无需单独订阅 ======

    this.subscribeUserOnlineStatus()
    
    // ====== 新增：订阅圈子消息（群聊） ======
    this.subscribeCircleMessagesInternal()
  }

  /**
   * 订阅圈子消息（群聊）- 内部方法
   */
  private subscribeCircleMessagesInternal(): void {
    if (!this.client) return

    const currentUserId = this.getCurrentUserId()
    
    console.log('🔵 [CircleChat] 开始订阅圈子消息，当前用户 ID:', currentUserId)
    
    // 订阅所有圈子的消息（使用通配符）
    const circleDestination = `/topic/circles/*/messages`
    
    this.client.subscribe(circleDestination, (message: IMessage) => {
      console.log('📨 [CircleChat] 收到圈子消息推送')
      console.log('📨 [CircleChat] 消息 body:', message.body)
      
      try {
        const data = JSON.parse(message.body)
        console.log('✅ [CircleChat] 解析后的消息对象:', data)
        console.log('✅ [CircleChat] 消息结构:', JSON.stringify(data, null, 2))
        
        // 触发 CIRCLE_CHAT_MESSAGE 处理器
        const handlers = this.messageHandlers.get('CIRCLE_CHAT_MESSAGE')
        
        console.log('🔍 [CircleChat] 查找处理器:', 
          '- 类型：CIRCLE_CHAT_MESSAGE',
          '- 是否存在：', !!handlers,
          '- 数量：', handlers?.size)
        
        if (handlers && handlers.size > 0) {
          let successCount = 0
          handlers.forEach(handler => {
            try {
              console.log('📤 [CircleChat] 执行处理器，传入数据:', data)
              handler(data)
              successCount++
            } catch (error) {
              console.error(`❌ [CircleChat] 圈子消息处理器执行出错:`, error)
            }
          })
          
          console.log('✅ [CircleChat] CIRCLE_CHAT_MESSAGE 处理器执行完成:', 
              '- 成功数量:', successCount,
              '- 总数量:', handlers.size)
        } else {
          console.warn('⚠️ [CircleChat] 未找到 CIRCLE_CHAT_MESSAGE 处理器！')
          console.warn('⚠️ [CircleChat] 当前所有注册的处理器:', Array.from(this.messageHandlers.keys()))
        }
      } catch (error) {
        console.error('❌ [CircleChat] 圈子消息解析失败:', error)
        console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
        console.error('  - 错误信息:', error instanceof Error ? error.message : error)
        console.error('  - 原始数据:', message.body)
      }
    }, {})
    
    console.log('✅ [CircleChat] 已订阅圈子消息广播:', circleDestination)
  }

  /**
   * 订阅用户在线状态队列
   */
  private subscribeUserOnlineStatus(): void {
    if (!this.client) return

    const currentUserId = this.getCurrentUserId()
    
    console.log('🔵 [OnlineStatus] 订阅在线状态，当前用户 ID:', currentUserId)
    
    // 订阅用户在线状态更新（后端主动推送）
    const onlineStatusDestination = `/user/${currentUserId}/queue/user-online-status`
    
    this.client.subscribe(onlineStatusDestination, (message: IMessage) => {
      console.log('📨 [OnlineStatus] 收到原始消息:', message)
      console.log('📨 [OnlineStatus] 消息 body:', message.body)
      
      try {
        const data = JSON.parse(message.body)
        console.log('✅ [WebSocket] 收到用户在线状态更新:', data)
        console.log('✅ [WebSocket] 解析后的数据结构:', JSON.stringify(data, null, 2))
        
        // 触发 USER_ONLINE_STATUS 处理器
        const handlers = this.messageHandlers.get('USER_ONLINE_STATUS')
        
        console.log('🔍 [WebSocket] 查找处理器:', 
          '- 类型：USER_ONLINE_STATUS',
          '- 是否存在：', !!handlers,
          '- 数量：', handlers?.size)
        
        if (handlers && handlers.size > 0) {
          let successCount = 0
          handlers.forEach(handler => {
            try {
              console.log('📤 [WebSocket] 执行处理器，传入数据:', data)
              handler(data)
              successCount++
            } catch (error) {
              console.error(`❌ [WebSocket] 在线状态处理器执行出错:`, error)
            }
          })
        } else {
          console.warn('⚠️ [WebSocket] 未找到 USER_ONLINE_STATUS 处理器！')
          console.warn('⚠️ [WebSocket] 当前所有注册的处理器:', Array.from(this.messageHandlers.keys()))
        }
      } catch (error) {
        console.error('❌ [WebSocket] 在线状态消息解析失败:', error)
        console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
        console.error('  - 错误信息:', error instanceof Error ? error.message : error)
        console.error('  - 原始数据:', message.body)
      }
    }, {})
    
    console.log('✅ [WebSocket] 已订阅用户在线状态队列:', onlineStatusDestination)

    // 可选：订阅用户列表（批量推送）
    const userListDestination = `/topic/online-users`
    
    this.client.subscribe(userListDestination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        console.log('✅ [WebSocket] 收到在线用户列表更新:', data)
        
        // 触发 USER_LIST_UPDATE 处理器
        const handlers = this.messageHandlers.get('USER_LIST_UPDATE')
        
        if (handlers && handlers.size > 0) {
          let successCount = 0
          handlers.forEach(handler => {
            try {
              handler(data)
              successCount++
            } catch (error) {
              console.error(`❌ [WebSocket] 用户列表处理器执行出错:`, error)
            }
          })

          console.log('✅ [WebSocket] USER_LIST_UPDATE 处理器执行完成:', 
              '- 成功数量:', successCount,
              '- 总数量:', handlers.size)
        } else {
          console.warn('⚠️ [WebSocket] 未找到 USER_LIST_UPDATE 处理器！')
        }
      } catch (error) {
        console.error('❌ [WebSocket] 在线用户列表解析失败:', error)
        console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
        console.error('  - 错误信息:', error instanceof Error ? error.message : error)
        console.error('  - 原始数据:', message.body)
      }
    }, {})
    
    console.log('✅ [WebSocket] 已订阅在线用户列表广播:', userListDestination)
  }

  /**
   * 请求获取指定用户的在线状态（主动查询）
   * @param userIds 用户 ID 数组
   */
  public queryUserOnlineStatus(userIds: number[]): void {
    if (!this.client || !this.client.connected) {
      console.warn('⚠️ [WebSocket] 未连接，无法查询用户在线状态')
      return
    }

    const request = {
      userIds: userIds,
    }

    console.log('🔍 [WebSocket] 查询用户在线状态:', request)
    
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

    console.log('📢 [WebSocket] 订阅好友在线状态')
    
    this.client.publish({
      destination: '/app/subscribe-friends-online-status',
      body: JSON.stringify({})
    })
  }

  /**
   * 发送私聊消息（关键方法！）- 按照后端文档实现
   */
  sendPrivateMessage(toUserId: number, content: string, msgType: number = 0): void {
    if (!this.client || !this.client.connected) {
      console.warn('⚠️ [WebSocket] 未连接，消息已跳过')
      return
    }
    
    const fromUserId = this.getCurrentUserId()
    if (!fromUserId) {
      console.error('❌ [WebSocket] 无法发送消息：未获取到当前用户 ID')
      return
    }
    
    //  关键：按照文档，直接发送纯 JSON 对象，不要包装
    const message: PrivateMessageDTO = {
      fromUserId,
      toUserId,
      content,
      msgType
    }

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
      console.warn('⚠️ [WebSocket.sendReadReceipt] WebSocket 未连接，无法发送已读回执')
      return
    }
    
    const currentUserId = this.getCurrentUserId()


    
    if (!currentUserId) {
      console.error('❌ [WebSocket.sendReadReceipt] 无法发送已读回执：未获取到当前用户 ID')

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
      })    } catch (error) {
      console.error('❌ [WebSocket.sendReadReceipt] 发送已读回执失败:', error)
      console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
      console.error('  - 错误信息:', error instanceof Error ? error.message : error)
    }

  }

  /**
   * 撤回消息
   */
  recallMessage(messageId: number, reason: string = ''): void {


    
    if (!this.client || !this.client.connected) {
      console.warn('⚠️ [WebSocket] 未连接，无法撤回消息')


      return
    }
    
    const currentUserId = this.getCurrentUserId()

    
    if (!currentUserId) {
      console.error('❌ [WebSocket] 无法撤回消息：未获取到当前用户 ID')
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
      })    } catch (error) {
      console.error('❌ [WebSocket.recallMessage] 发送撤回请求失败:', error)
      console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
      console.error('  - 错误信息:', error instanceof Error ? error.message : error)
    }

  }

  /**
   * 删除消息
   */
  deleteMessage(messageId: number): void {


    
    if (!this.client || !this.client.connected) {
      console.warn('⚠️ [WebSocket] 未连接，无法删除消息')


      return
    }
    
    const currentUserId = this.getCurrentUserId()

    
    if (!currentUserId) {
      console.error('❌ [WebSocket] 无法删除消息：未获取到当前用户 ID')
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
      })    } catch (error) {
      console.error('❌ [WebSocket.deleteMessage] 发送删除请求失败:', error)
      console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
      console.error('  - 错误信息:', error instanceof Error ? error.message : error)
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
    
    // PING 消息保持原有逻辑
    if (message.type === 'PING') {

      this.client.publish({
        destination: '/app/ping',
        body: JSON.stringify({ timestamp: Date.now() })
      })
    }
  }

  /**
   * 注册消息处理器（兼容旧接口）
   * @param type 消息类型：CHAT_MESSAGE | MESSAGE_STATUS | MESSAGE_RECALL | MESSAGE_DELETE | USER_ONLINE_STATUS | USER_LIST_UPDATE | CIRCLE_CHAT_MESSAGE | CIRCLE_CHAT_MESSAGE_DELETE
   */
  on(type: 'CHAT_MESSAGE' | 'MESSAGE_STATUS' | 'MESSAGE_RECALL' | 'MESSAGE_DELETE' | 'USER_ONLINE_STATUS' | 'USER_LIST_UPDATE' | 'CIRCLE_CHAT_MESSAGE' | 'CIRCLE_CHAT_MESSAGE_DELETE', handler: (data: any) => void): () => void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set())
    }
    
    this.messageHandlers.get(type)!.add(handler)
    console.log(`✅ [WebSocket] 已注册 ${type} 处理器`)
    
    // 返回取消订阅函数
    return () => {
      this.offMessage(type, handler)
    }
  }

  /**
   * 移除消息处理器
   */
  offMessage(type: 'CHAT_MESSAGE' | 'MESSAGE_STATUS' | 'MESSAGE_RECALL' | 'MESSAGE_DELETE' | 'USER_ONLINE_STATUS' | 'USER_LIST_UPDATE' | 'CIRCLE_CHAT_MESSAGE' | 'CIRCLE_CHAT_MESSAGE_DELETE', handler: (data: any) => void): void {
    const handlers = this.messageHandlers.get(type)
    if (handlers) {
      handlers.delete(handler)
      console.log(`❌ [WebSocket] 已移除 ${type} 处理器`)
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
   * 关闭 WebSocket 连接
   */
  close(): void {
    if (this.client) {

      this.isManualClose = true
      this.client.deactivate()
      this.client = null
    }
  }

  /**
   * 调度重连
   */
  private scheduleReconnect(): void {
    this.clearReconnectTimer()
    this.reconnectAttempts++

    
    this.reconnectTimer = setTimeout(() => {

      this.connect().catch(console.error)
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
        console.error('Error in WebSocket state listener:', error)
      }
    })
  }
}

// 创建全局单例
let wsManager: WebSocketManager | null = null

/**
 * 初始化 WebSocket
 * @param url WebSocket 服务器地址，例如：ws://localhost:8080/ws
 */
export function initWebSocket(url?: string): WebSocketManager {
  if (wsManager) {
    console.log('✅ [WebSocket] 使用已有实例')
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

  console.log('🔵 [WebSocket] 实例已创建（尚未连接）')
  return wsManager
}

/**
 * 获取 WebSocket 实例
 */
export function getWebSocket(): WebSocketManager | null {
  return wsManager
}

export default WebSocketManager
