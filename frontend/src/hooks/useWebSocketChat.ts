import { ref, onMounted, onUnmounted } from 'vue'
import chatService from '@/api/chat'
import type { Message, ConversationVO } from '@/types/message'
import { WsConnectionState } from '@/types/message'

/**
 * WebSocket 聊天功能的 Vue Composable
 * 提供响应式的 WebSocket 状态和消息处理
 */
export function useWebSocketChat() {
  const isConnected = ref(false)
  const connectionState = ref<WsConnectionState>(WsConnectionState.DISCONNECTED)
  const messages = ref<Message[]>([])
  const conversations = ref<ConversationVO[]>([])
  const unreadCount = ref(0)

  let unsubscribeMessage: (() => void) | null = null
  let unsubscribeStatus: (() => void) | null = null
  let unsubscribeConversation: (() => void) | null = null
  let unsubscribeUnread: (() => void) | null = null
  let unsubscribeConnection: (() => void) | null = null

  /**
   * 连接 WebSocket
   */
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

  /**
   * 断开连接
   */
  const disconnect = () => {
    chatService.disconnect()
    connectionState.value = WsConnectionState.DISCONNECTED
    isConnected.value = false
  }

  /**
   * 添加消息到列表
   */
  const addMessage = (message: Message) => {
    messages.value = [...messages.value, message]
  }

  /**
   * 更新消息状态
   */
  const updateMessageStatus = (messageId: number, status: string) => {
    messages.value = messages.value.map(msg => 
      msg.id === messageId ? { ...msg, status: status as Message['status'] } : msg
    )
  }

  /**
   * 更新会话列表
   */
  const updateConversation = (conversationId: number) => {
    // 重新加载会话列表
    loadConversations()
  }

  /**
   * 加载会话列表
   */
  const loadConversations = async () => {
    try {
      const list = await chatService.getConversations()
      conversations.value = list
      
      // 计算总未读数
      unreadCount.value = list.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
    } catch (error) {
      console.error('Failed to load conversations:', error)
    }
  }

  /**
   * 加载聊天记录
   */
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

  /**
   * 发送消息
   */
  const sendMessage = async (toUserId: number, content: string, contentType: 'TEXT' | 'IMAGE' | 'FILE' = 'TEXT') => {
    try {
      const response = await chatService.sendMessage({
        toUserId,
        content,
        contentType
      })
      
      // 如果是新会话，刷新会话列表
      if (response.shouldCreateConversation) {
        loadConversations()
      }
      
      return response
    } catch (error) {
      console.error('Failed to send message:', error)
      throw error
    }
  }

  /**
   * 标记为已读
   */
  const markAsRead = async (fromUserId: number) => {
    try {
      await chatService.markAsRead(fromUserId)
      loadConversations()
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  /**
   * 设置消息监听器
   */
  const setupListeners = () => {
    // 监听新消息
    unsubscribeMessage = chatService.onNewMessage((message) => {
      addMessage(message)
      loadConversations()
    })

    // 监听消息状态更新
    unsubscribeStatus = chatService.onMessageStatusUpdate(({ messageId, status }) => {
      updateMessageStatus(messageId, status)
    })

    // 监听新会话
    unsubscribeConversation = chatService.onNewConversation(({ conversationId }) => {
      updateConversation(conversationId)
    })

    // 监听未读数更新
    unsubscribeUnread = chatService.onUnreadCountUpdate(({ conversationId, unreadCount: count }) => {
      loadConversations()
    })

    // 监听连接状态
    unsubscribeConnection = chatService.onConnectionChange((state) => {
      connectionState.value = state
      isConnected.value = state === WsConnectionState.CONNECTED
    })
  }

  /**
   * 清理监听器
   */
  const cleanupListeners = () => {
    unsubscribeMessage?.()
    unsubscribeStatus?.()
    unsubscribeConversation?.()
    unsubscribeUnread?.()
    unsubscribeConnection?.()
  }

  /**
   * 生命周期挂载
   */
  onMounted(() => {
    setupListeners()
    loadConversations()
  })

  /**
   * 生命周期卸载
   */
  onUnmounted(() => {
    cleanupListeners()
    disconnect()
  })

  return {
    // 状态
    isConnected,
    connectionState,
    messages,
    conversations,
    unreadCount,
    
    // 方法
    connect,
    disconnect,
    loadConversations,
    loadChatHistory,
    sendMessage,
    markAsRead
  }
}

export default useWebSocketChat
