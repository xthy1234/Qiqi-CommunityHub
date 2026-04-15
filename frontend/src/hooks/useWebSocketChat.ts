import { ref, onMounted, onUnmounted } from 'vue'
import chatService from '@/api/chat'
import type { 
  Message, 
  ConversationVO,
  MessageStatusPayload,
  NewConversationPayload,
  UnreadCountUpdatePayload
} from '@/types/message'
import { WsConnectionState } from '@/types/message'
import { getWebSocket } from '@/utils/websocket'
import { wsLogger } from '@/utils/websocketLogger'
import { debounce } from '@/utils/function'

export function useWebSocketChat() {
  const isConnected = ref(false)
  const connectionState = ref<WsConnectionState>(WsConnectionState.DISCONNECTED)
  const messages = ref<Message[]>([])
  const conversations = ref<ConversationVO[]>([])
  const unreadCount = ref(0)
  const currentChatUserId = ref<number | null>(null)

  let cleanupFns: (() => void)[] = []

  const connect = async (wsUrl?: string) => {
    try {
      connectionState.value = WsConnectionState.CONNECTING
      wsLogger.logConnectionState('CONNECTING')
      // 注意：WebSocket 应该在登录时已初始化，这里只是确保连接
      const ws = getWebSocket()
      if (ws && !ws.isConnected()) {
        await ws.connect()
      }
    } catch (error) {
      wsLogger.error('WebSocket 连接失败', { error })
      connectionState.value = WsConnectionState.DISCONNECTED
      throw error
    }
  }

  const disconnect = () => {
    const ws = getWebSocket()
    if (ws) {
      ws.close()
    }
    connectionState.value = WsConnectionState.DISCONNECTED
    isConnected.value = false
    wsLogger.logConnectionState('DISCONNECTED')
  }

  const updateMessageStatus = (messageId: number, status: string) => {
    const msg = messages.value.find(m => m.id === messageId)
    if (msg) {
      msg.status = status as any
    }
  }

  const updateConversation = (conversationId: number) => {
    loadConversations()
  }

  const loadConversations = async () => {
    try {
      const data = await chatService.getConversations()
      conversations.value = Array.isArray(data) ? data : []
      unreadCount.value = conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
    } catch (error) {
      wsLogger.error('加载会话列表失败', { error })
    }
  }

  const loadChatHistory = async (userId: number, page = 1, limit = 20) => {
    try {
      const result = await chatService.getChatHistory(userId, page, limit)
      if (page === 1) {
        messages.value = result.list
      } else {
        messages.value = [...result.list, ...messages.value]
      }
      return result
    } catch (error) {
      wsLogger.error('加载聊天记录失败', { error })
      return { list: [], total: 0 }
    }
  }

  const sendMessage = async (toUserId: number, content: string, contentType: 'TEXT' | 'IMAGE' | 'FILE' = 'TEXT') => {
    try {
      const response = await chatService.sendMessage({
        toUserId,
        content,
        contentType
      })
      
      if (response.shouldCreateConversation) {
        loadConversations()
      }
      
      return response
    } catch (error) {
      wsLogger.error('发送消息失败', { error })
      throw error
    }
  }

  const debouncedMarkAsRead = debounce(async (fromUserId: number, reason = '手动触发') => {
    try {
      const ws = getWebSocket()
      if (ws && ws.isConnected()) {
        ws.sendReadReceipt(fromUserId)
        wsLogger.debug('已读回执已发送', { fromUserId, reason })
      } else {
        wsLogger.warn('WebSocket 未连接，无法发送已读回执')
      }
      
      loadConversations()
    } catch (error) {
      wsLogger.error('标记已读失败', { error })
    }
  }, 300)

  const markAsRead = async (fromUserId: number, reason = '手动触发') => {
    debouncedMarkAsRead(fromUserId, reason)
  }

  const setCurrentChatUser = (userId: number | null) => {
    currentChatUserId.value = userId
  }

  const setupListeners = () => {
    // 保留原有的其他监听器
    cleanupFns.push(
      chatService.onNewMessage((message: Message) => {
        messages.value.push(message)
        loadConversations()
        wsLogger.debug('收到私聊消息', { 
          fromUserId: message.fromUserId,
          toUserId: message.toUserId,
          content: typeof message.content === 'string' ? message.content.substring(0, 50) : '[object]' 
        })
      }),

      chatService.onMessageStatusUpdate((data: MessageStatusPayload['data']) => {
        updateMessageStatus(data.messageId, data.status)
        wsLogger.debug('消息状态更新', { messageId: data.messageId, status: data.status })
      }),

      chatService.onNewConversation((data: NewConversationPayload['data']) => {
        updateConversation(data.conversationId)
        wsLogger.debug('新会话创建', { conversationId: data.conversationId })
      }),

      chatService.onUnreadCountUpdate((data: UnreadCountUpdatePayload['data']) => {
        loadConversations()
        wsLogger.debug('未读数更新', { conversationId: data.conversationId, count: data.unreadCount })
      }),

      chatService.onConnectionChange((state: WsConnectionState) => {
        connectionState.value = state
        isConnected.value = state === WsConnectionState.CONNECTED
        
        wsLogger.logConnectionState(
          state === WsConnectionState.CONNECTED ? 'CONNECTED' :
          state === WsConnectionState.CONNECTING ? 'CONNECTING' :
          state === WsConnectionState.RECONNECTING ? 'RECONNECTING' : 'DISCONNECTED'
        )
        
        if (state === WsConnectionState.CONNECTED && currentChatUserId.value) {
          setTimeout(() => {
            markAsRead(currentChatUserId.value!, 'WebSocket 重连成功')
          }, 500)
        }
      })
    )
  }

  const cleanupListeners = () => {
    cleanupFns.forEach(fn => fn())
    cleanupFns = []
    wsLogger.info('清理所有监听器')
  }

  onMounted(() => {
    setupListeners()
  })

  onUnmounted(() => {
    cleanupListeners()
  })

  return {
    isConnected,
    connectionState,
    messages,
    conversations,
    unreadCount,
    connect,
    disconnect,
    markAsRead,
    setCurrentChatUser,
    loadConversations,
    loadChatHistory,
    sendMessage
  }
}

export default useWebSocketChat
