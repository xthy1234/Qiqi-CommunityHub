import type { CircleMessage } from '@/types/circleChat'
import { getWebSocket } from '@/utils/websocket'
import { createStompSubscription } from '@/utils/websocketSubscription'
import { wsLogger } from '@/utils/websocketLogger'

/**
 * 圈子消息服务（纯业务逻辑，无 Vue 依赖）
 */
class CircleChatMessageService {
  private messageHandlers: Set<(message: CircleMessage) => void> = new Set()
  private deleteHandlers: Set<(data: { messageId: number; deleterId: number; deleterNickname?: string }) => void> = new Set()
  private unsubscribeMessages: (() => void) | null = null
  private unsubscribeDelete: (() => void) | null = null

  /**
   * 初始化圈子消息订阅
   */
  init(): void {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      wsLogger.warn('WebSocket 未连接，无法初始化圈子消息服务')
      return
    }

    const client = (ws as any).client
    if (!client) return

    wsLogger.info('初始化圈子消息服务')

    // 订阅圈子消息（使用通配符订阅所有圈子）
    this.unsubscribeMessages = createStompSubscription(client, {
      messageType: 'CIRCLE_CHAT_MESSAGE',
      destination: '/topic/circles/*/messages',
      handler: (data: CircleMessage) => {
        wsLogger.logMessageReceived('CIRCLE_CHAT_MESSAGE', JSON.stringify(data).length)
        wsLogger.debug('收到圈子消息', {
          circleId: data.circleId,
          senderId: data.senderId,
          msgType: data.msgType
        })
        this.messageHandlers.forEach(h => h(data))
      },
      logPrefix: 'CircleChatService'
    })

    // 订阅圈子消息删除通知
    this.unsubscribeDelete = createStompSubscription(client, {
      messageType: 'CIRCLE_CHAT_MESSAGE_DELETE',
      destination: '/topic/circles/*/messages/delete',
      handler: (data) => {
        wsLogger.debug('收到圈子消息删除通知', data)
        this.deleteHandlers.forEach(h => h(data))
      },
      logPrefix: 'CircleChatService'
    })
  }

  /**
   * 监听新消息
   */
  onMessage(handler: (message: CircleMessage) => void): () => void {
    this.messageHandlers.add(handler)
    return () => this.messageHandlers.delete(handler)
  }

  /**
   * 监听消息删除
   */
  onDelete(handler: (data: { messageId: number; deleterId: number; deleterNickname?: string }) => void): () => void {
    this.deleteHandlers.add(handler)
    return () => this.deleteHandlers.delete(handler)
  }

  /**
   * 清理订阅
   */
  destroy(): void {
    this.unsubscribeMessages?.()
    this.unsubscribeDelete?.()
    this.messageHandlers.clear()
    this.deleteHandlers.clear()

    wsLogger.info('圈子消息服务已清理')
  }
}

export default new CircleChatMessageService()
