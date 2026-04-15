import type { Message } from '@/types/message'
import { getWebSocket } from '@/utils/websocket'
import { createStompSubscription } from '@/utils/websocketSubscription'
import { wsLogger } from '@/utils/websocketLogger'

/**
 * 私聊消息服务（纯业务逻辑，无 Vue 依赖）
 * 职责：提供事件监听接口，不负责订阅管理
 * 订阅由 WebSocketManager 统一管理
 */
class ChatMessageService {
  private messageHandlers: Set<(message: Message) => void> = new Set()
  private statusHandlers: Set<(data: { messageId: number; status: 'SENT' | 'DELIVERED' | 'READ' }) => void> = new Set()
  private recallHandlers: Set<(data: { messageId: number; userId: number; reason?: string }) => void> = new Set()
  private deleteHandlers: Set<(data: { messageId: number; userId: number }) => void> = new Set()
  


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
    
    this.messageHandlers.clear()
    this.statusHandlers.clear()
    this.recallHandlers.clear()
    this.deleteHandlers.clear()
    
    wsLogger.info('聊天消息服务已清理')
  }
}

export default new ChatMessageService()
