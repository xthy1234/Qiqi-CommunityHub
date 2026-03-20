import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'
import type { 
  Message, 
  MessageSendDTO, 
  MessageSendResponseVO, 
  ConversationVO,
  WsChatMessageType
} from '@/types/message'
import { WsConnectionState } from '@/types/message'
import WebSocketManager, {
  initWebSocket,
  getWebSocket
} from '@/utils/websocket'

/**
 * WebSocket 消息服务类
 * 整合了 HTTP API 和 WebSocket 实时通信
 */
class ChatService {
  private baseUrl = '/messages'
  private wsManager: WebSocketManager | null = null
  private messageHandlers: Map<string, Set<(data: any) => void>> = new Map()

  /**
   * 初始化 WebSocket 连接
   * @param wsUrl WebSocket 服务器地址，默认使用项目配置
   */
  connect(wsUrl?: string): Promise<void> {
    this.wsManager = initWebSocket(wsUrl)
    return this.wsManager.connect()
  }

  /**
   * 断开 WebSocket 连接
   */
  disconnect(): void {
    if (this.wsManager) {
      this.wsManager.close()
      this.wsManager = null
    }
  }

  /**
   * 获取 WebSocket 实例
   */
  getWebSocket(): WebSocketManager | null {
    return getWebSocket()
  }

  /**
   * 检查 WebSocket 连接状态
   */
  isConnected(): boolean {
    const ws = getWebSocket()
    return ws ? ws.isConnected() : false
  }

  /**
   * 监听 WebSocket 连接状态
   */
  onConnectionChange(callback: (state: WsConnectionState) => void): () => void {
    const ws = getWebSocket()
    if (!ws) {
      return () => {}
    }

    return ws.onStateChange((state) => {
      const stateMap = {
        0: WsConnectionState.CONNECTING,
        1: WsConnectionState.CONNECTED,
        2: WsConnectionState.RECONNECTING,
        3: WsConnectionState.DISCONNECTED
      }
      callback(stateMap[state])
    })
  }

  /**
   * 监听新消息
   */
  onNewMessage(handler: (message: Message) => void): () => void {
    return this.registerHandler('CHAT_MESSAGE', handler)
  }

  /**
   * 监听消息状态更新
   */
  onMessageStatusUpdate(handler: (data: { messageId: number; status: string }) => void): () => void {
    return this.registerHandler('MESSAGE_STATUS', handler)
  }

  /**
   * 监听新会话
   */
  onNewConversation(handler: (data: { conversationId: number }) => void): () => void {
    return this.registerHandler('NEW_CONVERSATION', handler)
  }

  /**
   * 监听未读消息数更新
   */
  onUnreadCountUpdate(handler: (data: { conversationId: number; unreadCount: number }) => void): () => void {
    return this.registerHandler('UNREAD_COUNT_UPDATE', handler)
  }

  /**
   * 监听用户在线状态更新
   */
  onUserOnlineStatus(handler: (data: { userId: number; online: boolean; timestamp?: number; lastSeenAt?: string }) => void): () => void {
    return this.registerHandler('USER_ONLINE_STATUS', handler)
  }

  /**
   * 监听在线用户列表更新
   */
  onUserListUpdate(handler: (data: { users: Array<{ userId: number; online: boolean; lastSeenAt?: string }>; timestamp: number }) => void): () => void {
    return this.registerHandler('USER_LIST_UPDATE', handler)
  }

  /**
   * 查询用户在线状态
   */
  queryUserOnlineStatus(userIds: number[]): void {
    const ws = getWebSocket()
    if (ws?.isConnected()) {
      ws.queryUserOnlineStatus(userIds)
    } else {
      console.warn('WebSocket 未连接，无法查询用户在线状态')
    }
  }

  /**
   * 订阅好友在线状态
   */
  subscribeFriendsOnlineStatus(): void {
    const ws = getWebSocket()
    if (ws?.isConnected()) {
      ws.subscribeFriendsOnlineStatus()
    } else {
      console.warn('WebSocket 未连接，无法订阅好友在线状态')
    }
  }

  /**
   * 注册消息处理器
   */
  private registerHandler(messageType: string, handler: (data: any) => void): () => void {
    const ws = getWebSocket()
    if (!ws) {
      console.warn('WebSocket not initialized')
      return () => {}
    }

    return ws.on(messageType as any, handler)
  }

  /**
   * 发送私信（通过 HTTP API）
   * @param params 发送消息参数
   */
  async sendMessage(params: MessageSendDTO): Promise<MessageSendResponseVO> {
    const response: AxiosResponse<ApiResponse<MessageSendResponseVO>> = await httpClient.post(this.baseUrl, params)
    return response.data.data
  }

  /**
   * 获取聊天记录
   * @param userId 对方用户 ID
   * @param page 页码
   * @param limit 每页数量
   */
  async getChatHistory(userId: number, page: number = 1, limit: number = 20): Promise<{ list: Message[]; total: number }> {
    const response: AxiosResponse<ApiResponse<{ list: Message[]; total: number }>> = await httpClient.get(`${this.baseUrl}/with/${userId}`, {
      params: { page, limit }
    })
    return response.data.data
  }

  /**
   * 获取与指定用户的会话信息
   * @param userId 对方用户 ID
   */
  async getConversationWithUser(userId: number): Promise<ConversationVO | null> {
    try {
      const response: AxiosResponse<ApiResponse<ConversationVO>> = await httpClient.get(`${this.baseUrl}/conversation/${userId}`)
      return response.data.data
    } catch (error) {
      return null
    }
  }

  /**
   * 获取会话列表
   */
  async getConversations(): Promise<ConversationVO[]> {
    const response: AxiosResponse<ApiResponse<{ list: ConversationVO[]; totalCount?: number }>> = await httpClient.get(`${this.baseUrl}/conversations`)

    if (response.data && response.data.data) {
      const data = response.data.data
      if (typeof data === 'object' && 'list' in data) {
        return Array.isArray(data.list) ? data.list : []
      }
      return Array.isArray(data) ? data : []
    }
    return []
  }

  /**
   * 标记消息已读
   * @param fromUserId 消息发送方用户 ID
   */
  async markAsRead(fromUserId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`${this.baseUrl}/read`, { fromUserId })
    return response.data.data
  }

  /**
   * 删除单条消息
   * @param messageId 消息 ID
   */
  async deleteMessage(messageId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`${this.baseUrl}/${messageId}`)
    return response.data.data
  }

  /**
   * 批量删除消息
   * @param messageIds 消息 ID 数组
   */
  async batchDeleteMessages(messageIds: number[]): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`${this.baseUrl}/batch-delete`, { ids: messageIds })
    return response.data.data
  }
}

export default new ChatService()
