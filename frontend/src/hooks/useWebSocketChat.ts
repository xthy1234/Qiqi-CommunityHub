import { ref, onMounted, onUnmounted, watch } from 'vue'
import chatService from '@/api/chat'
import type { Message, ConversationVO } from '@/types/message'
import { WsConnectionState } from '@/types/message'
import { getWebSocket } from '@/utils/websocket'
import { MessageStatus, isUnreadMessage } from '@/types/message'
import { debounce } from '@/utils/function'

export function useWebSocketChat() {
  const isConnected = ref(false)
  const connectionState = ref<WsConnectionState>(WsConnectionState.DISCONNECTED)
  const messages = ref<Message[]>([])
  const conversations = ref<ConversationVO[]>([])
  const unreadCount = ref(0)
  const currentChatUserId = ref<number | null>(null)

  let unsubscribeMessage: (() => void) | null = null
  let unsubscribeStatus: (() => void) | null = null
  let unsubscribeConversation: (() => void) | null = null
  let unsubscribeUnread: (() => void) | null = null
  let unsubscribeConnection: (() => void) | null = null

  const connect = async (wsUrl?: string) => {
    try {
      connectionState.value = WsConnectionState.CONNECTING
      await chatService.connect(wsUrl)
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      connectionState.value = WsConnectionState.DISCONNECTED
      throw error
    }
  }

  const disconnect = () => {
    chatService.disconnect()
    connectionState.value = WsConnectionState.DISCONNECTED
    isConnected.value = false
  }

  const addMessage = (message: Message) => {
    messages.value.push(message)
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
      console.error('Failed to load conversations:', error)
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
      console.error('Failed to load chat history:', error)
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
      console.error('Failed to send message:', error)
      throw error
    }
  }

  const debouncedMarkAsRead = debounce(async (fromUserId: number, reason: string = '手动触发') => {
    try {
      const ws = getWebSocket()
      if (ws && ws.isConnected()) {
        ws.sendReadReceipt(fromUserId)

      } else {
        console.warn('⚠️ [useWebSocketChat] WebSocket 未连接，无法发送已读回执')
      }
      
      loadConversations()
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }, 300)

  const markAsRead = async (fromUserId: number, reason: string = '手动触发') => {
    debouncedMarkAsRead(fromUserId, reason)
  }

  const setCurrentChatUser = (userId: number | null) => {
    currentChatUserId.value = userId
  }

  const setupListeners = () => {
    unsubscribeMessage = chatService.onNewMessage((message) => {
      addMessage(message)
      loadConversations()
      
      //  关键修改：移除自动发送已读回执的逻辑
      // 改为由 ChatDetail 组件根据实际页面状态决定是否发送

    })

    unsubscribeStatus = chatService.onMessageStatusUpdate(({ messageId, status }) => {
      updateMessageStatus(messageId, status)
    })

    unsubscribeConversation = chatService.onNewConversation(({ conversationId }) => {
      updateConversation(conversationId)
    })

    unsubscribeUnread = chatService.onUnreadCountUpdate(({ conversationId, unreadCount: count }) => {
      loadConversations()
    })

    unsubscribeConnection = chatService.onConnectionChange((state: WsConnectionState) => {
      connectionState.value = state
      isConnected.value = state === WsConnectionState.CONNECTED
      
      if (state === WsConnectionState.CONNECTED && currentChatUserId.value) {
        setTimeout(() => {
          markAsRead(currentChatUserId.value!, 'WebSocket 重连成功')
        }, 500)
      }
    })
  }

  const cleanupListeners = () => {
    unsubscribeMessage?.()
    unsubscribeStatus?.()
    unsubscribeConversation?.()
    unsubscribeUnread?.()
    unsubscribeConnection?.()
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
