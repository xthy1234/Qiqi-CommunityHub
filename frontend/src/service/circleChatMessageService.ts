import type { CircleMessage } from '@/types/circleChat'
import { getWebSocket } from '@/utils/websocket'
import { createStompSubscription } from '@/utils/websocketSubscription'
import { wsLogger } from '@/utils/websocketLogger'

/**
 * 圈子消息服务（纯业务逻辑，无 Vue 依赖）
 * 职责：提供事件监听接口，不负责订阅管理
 * 订阅由 WebSocketManager 统一管理
 */
class CircleChatMessageService {
  private messageHandlers: Set<(message: CircleMessage) => void> = new Set()
  private deleteHandlers: Set<(data: { messageId: number; deleterId: number; deleterNickname?: string }) => void> = new Set()
  
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
    this.messageHandlers.clear()
    this.deleteHandlers.clear()

    wsLogger.info('圈子消息服务已清理')
  }
}

export default new CircleChatMessageService()
