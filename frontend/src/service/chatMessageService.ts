import type { Message } from '@/types/message'
import { getWebSocket } from '@/utils/websocket'
import { createStompSubscription } from '@/utils/websocketSubscription'
import { wsLogger } from '@/utils/websocketLogger'

/**
 * 私聊消息服务（纯业务逻辑，无 Vue 依赖）
 */
class ChatMessageService {
  private messageHandlers: Set<(message: Message) => void> = new Set()
  private statusHandlers: Set<(data: { messageId: number; status: 'SENT' | 'DELIVERED' | 'READ' }) => void> = new Set()
  private recallHandlers: Set<(data: { messageId: number; userId: number; reason?: string }) => void> = new Set()
  private deleteHandlers: Set<(data: { messageId: number; userId: number }) => void> = new Set()
  
  private unsubscribeMessages: (() => void) | null = null
  private unsubscribeStatus: (() => void) | null = null
  private unsubscribeRecall: (() => void) | null = null
  private unsubscribeDelete: (() => void) | null = null

  /**
   * 初始化私聊消息订阅
   */
  init(userId: number): void {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      wsLogger.warn('WebSocket 未连接，无法初始化聊天消息服务')
      return
    }

    const client = (ws as any).client
    if (!client) return

    wsLogger.info('初始化聊天消息服务', { userId })

    // 订阅私聊消息
    this.unsubscribeMessages = createStompSubscription(client, {
      messageType: 'CHAT_MESSAGE',
      destination: `/user/${userId}/queue/private-messages`,
      handler: (data: Message) => {
        wsLogger.logMessageReceived('CHAT_MESSAGE', JSON.stringify(data).length)
        wsLogger.debug('收到私聊消息', {
          fromUserId: data.fromUserId,
          toUserId: data.toUserId,
          contentPreview: typeof data.content === 'string' ? data.content.substring(0, 30) : '[object]'
        })
        this.messageHandlers.forEach(h => h(data))
      },
      logPrefix: 'ChatMessageService'
    })

    // 订阅消息状态更新（已读回执）
    this.unsubscribeStatus = createStompSubscription(client, {
      messageType: 'MESSAGE_STATUS',
      destination: `/user/${userId}/queue/read-receipts`,
      handler: (data) => {
        wsLogger.debug('收到消息状态更新', data)
        this.statusHandlers.forEach(h => h(data))
      },
      logPrefix: 'ChatMessageService'
    })

    // 订阅消息撤回通知
    this.unsubscribeRecall = createStompSubscription(client, {
      messageType: 'MESSAGE_RECALL',
      destination: `/user/${userId}/queue/message-recall`,
      handler: (data) => {
        wsLogger.debug('收到消息撤回通知', data)
        this.recallHandlers.forEach(h => h(data))
      },
      logPrefix: 'ChatMessageService'
    })

    // 订阅消息删除通知
    this.unsubscribeDelete = createStompSubscription(client, {
      messageType: 'MESSAGE_DELETE',
      destination: `/user/${userId}/queue/message-delete`,
      handler: (data) => {
        wsLogger.debug('收到消息删除通知', data)
        this.deleteHandlers.forEach(h => h(data))
      },
      logPrefix: 'ChatMessageService'
    })
  }

  /**
   * 监听新消息
   */
  onMessage(handler: (message: Message) => void): () => void {
    this.messageHandlers.add(handler)
    return () => this.messageHandlers.delete(handler)
  }

  /**
   * 监听消息状态更新
   */
  onStatusUpdate(handler: (data: { messageId: number; status: 'SENT' | 'DELIVERED' | 'READ' }) => void): () => void {
    this.statusHandlers.add(handler)
    return () => this.statusHandlers.delete(handler)
  }

  /**
   * 监听消息撤回
   */
  onRecall(handler: (data: { messageId: number; userId: number; reason?: string }) => void): () => void {
    this.recallHandlers.add(handler)
    return () => this.recallHandlers.delete(handler)
  }

  /**
   * 监听消息删除
   */
  onDelete(handler: (data: { messageId: number; userId: number }) => void): () => void {
    this.deleteHandlers.add(handler)
    return () => this.deleteHandlers.delete(handler)
  }

  /**
   * 清理订阅
   */
  destroy(): void {
    this.unsubscribeMessages?.()
    this.unsubscribeStatus?.()
    this.unsubscribeRecall?.()
    this.unsubscribeDelete?.()
    
    this.messageHandlers.clear()
    this.statusHandlers.clear()
    this.recallHandlers.clear()
    this.deleteHandlers.clear()
    
    wsLogger.info('聊天消息服务已清理')
  }
}

export default new ChatMessageService()
