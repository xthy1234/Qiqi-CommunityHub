
import { defineStore } from 'pinia'
import type { Message, ConversationVO } from '@/types/message'
import messageService from '@/api/message'

interface ChatState {
  conversations: ConversationVO[]
  currentConversation: ConversationVO | null
  messages: Message[]
  unreadCount: number
  loading: boolean
  messagePage: number
  hasMore: boolean
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    conversations: [],
    currentConversation: null,
    messages: [],
    unreadCount: 0,
    loading: false,
    messagePage: 1,
    hasMore: true
  }),

  getters: {
    /** 获取当前聊天对象的消息列表 */
    currentMessages: (state): Message[] => {
      return state.messages
    },

    /** 检查是否有未读消息 */
    hasUnreadMessages: (state): boolean => {
      return state.unreadCount > 0
    }
  },

  actions: {
    /** 初始化聊天状态 */
    initializeChatState() {
      this.conversations = []
      this.currentConversation = null
      this.messages = []
      this.unreadCount = 0
      this.loading = false
      this.messagePage = 1
      this.hasMore = true
    },

    /** 加载会话列表 */
    async loadConversations() {
      try {
        this.loading = true
        const data = await messageService.getConversations()
        
        // 确保 data 是数组类型
        this.conversations = Array.isArray(data) ? data : []
        
        // 计算总未读数
        this.unreadCount = this.conversations.reduce((sum: number, conv: ConversationVO) => sum + (conv.unreadCount || 0), 0)
      } catch (error) {
        console.error('加载会话列表失败:', error)
        this.conversations = []
        this.unreadCount = 0
        throw error
      } finally {
        this.loading = false
      }
    },

    /** 切换当前聊天对象 */
    async switchConversation(conversation: ConversationVO) {
      try {
        if (!conversation || !conversation.userId) {
          console.error('无效的会话对象')
          return
        }
        
        this.currentConversation = conversation
        this.messagePage = 1
        this.hasMore = true
        
        // 加载聊天记录
        await this.loadMessages(conversation.userId, true)
        
        // 标记为已读
        if (conversation.unreadCount && conversation.unreadCount > 0) {
          await messageService.markAsRead(conversation.userId)
          // 更新本地未读数
          this.unreadCount -= conversation.unreadCount
          conversation.unreadCount = 0
          
          // 更新会话列表中的未读数
          const index = this.conversations.findIndex((c: ConversationVO) => c.userId === conversation.userId)
          if (index !== -1) {
            this.conversations[index].unreadCount = 0
          }
        }
      } catch (error) {
        console.error('切换会话失败:', error)
        throw error
      }
    },

    /** 加载消息列表 */
    async loadMessages(userId: number, reset: boolean = false) {
      try {
        if (reset) {
          this.messagePage = 1
          this.messages = []
        }

        if (!this.hasMore) return

        this.loading = true
        const page = this.messagePage
        const limit = 20

        const result = await messageService.getChatHistory(userId, page, limit)
        
        if (reset) {
          this.messages = result.list.reverse() // 最新的在底部
        } else {
          // 加载更多时，将旧消息添加到前面
          this.messages = [...result.list.reverse(), ...this.messages]
        }

        this.hasMore = result.list.length === limit
        this.messagePage++
      } catch (error) {
        console.error('加载消息失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    /** 添加发送的消息 */
    addSentMessage(message: Message) {
      this.messages.push(message)
      
      // 更新会话列表
      const index = this.conversations.findIndex((c: ConversationVO) => c.userId === message.toUserId)
      if (index !== -1) {
        this.conversations.splice(index, 1)
      }
      
      // 添加到最前面，确保有用户名和头像
      const conversationData: ConversationVO = {
        userId: message.toUserId,
        username: this.currentConversation?.username || '未知用户',
        avatar: this.currentConversation?.avatar || '',
        lastMessage: message.content,
        lastTime: message.createTime,
        unreadCount: 0
      }
      
      this.conversations.unshift(conversationData)
    },

    /** 接收新消息（WebSocket） */
    receiveMessage(message: Message) {
      // 如果当前正在和该用户聊天，直接添加到消息列表
      if (this.currentConversation && 
          (message.fromUserId === this.currentConversation.userId || 
           message.toUserId === this.currentConversation.userId)) {
        this.messages.push(message)
        
        // 更新会话列表
        const index = this.conversations.findIndex((c: ConversationVO) => c.userId === message.fromUserId)
        if (index !== -1) {
          this.conversations[index].lastMessage = message.content
          this.conversations[index].lastTime = message.createTime
          
          // 如果当前窗口打开，标记为已读
          if (this.currentConversation.userId === message.fromUserId) {
            this.conversations[index].unreadCount = 0
          } else {
            this.conversations[index].unreadCount++
            this.unreadCount++
          }
          
          // 移动到最前面
          const conv = this.conversations.splice(index, 1)[0]
          this.conversations.unshift(conv)
        }
      } else {
        // 否则只更新会话列表和未读数
        const index = this.conversations.findIndex((c: ConversationVO) => c.userId === message.fromUserId)
        if (index !== -1) {
          this.conversations[index].lastMessage = message.content
          this.conversations[index].lastTime = message.createTime
          this.conversations[index].unreadCount++
          this.unreadCount++
          
          // 移动到最前面
          const conv = this.conversations.splice(index, 1)[0]
          this.conversations.unshift(conv)
        }
      }
    },

    /** 加载更多历史消息 */
    async loadMoreMessages() {
      if (this.currentConversation) {
        await this.loadMessages(this.currentConversation.userId, false)
      }
    },

    /** 清空聊天记录 */
    async clearChatHistory(userId: number) {
      try {
        // TODO: 调用后端删除接口
        this.messages = []
      } catch (error) {
        console.error('清空聊天记录失败:', error)
        throw error
      }
    }
  }
})
