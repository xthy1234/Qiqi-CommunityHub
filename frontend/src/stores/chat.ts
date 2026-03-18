
import { defineStore } from 'pinia'
import type { Message, ConversationVO } from '@/types/message'
import messageService from '@/api/message'
import userService from '@/api/user'

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

    /** 更新消息状态 */
    updateMessageStatus(messageId: number, status: string) {
      const message = this.messages.find((m: Message) => m.id === messageId || m._tempId === messageId.toString())
      if (message) {
        message.status = status as any
        message._sending = false
// console.log('📊 [chatStore] 消息状态已更新:', messageId, status)
      }
    },

    /** 
     * 乐观添加发送中的消息（关键方法！）
     * @returns 临时消息对象，包含_tempId
     */
    addSendingMessage(content: string, toUserId: number): Message {
      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      
      // 🔥 从 localStorage 获取当前用户 ID
      const useridStr = localStorage.getItem('userid')
      const currentUserId = Number(useridStr || '0')
      
      console.log('🟢 [Store.addSendingMessage] 开始添加发送中消息:', {
        tempId,
        content,
        toUserId,
        currentUserId
      })
      
      const tempMessage: Message = {
        id: 0, // 临时 ID 为 0
        _tempId: tempId,
        _sending: true,
        fromUserId: currentUserId,
        toUserId: toUserId,
        content: content,
        msgType: 0,
        status: 'SENDING',
        createTime: new Date().toISOString(),
        isSelf: true
      }
      
      this.messages.push(tempMessage)
      console.log('✅ [Store.addSendingMessage] 消息已添加到 messages 数组，当前消息数:', this.messages.length)
      console.log('📋 [Store.addSendingMessage] 临时消息详情:', tempMessage)
      
      // 更新会话列表
      const index = this.conversations.findIndex((c: ConversationVO) => c.userId === toUserId)
      if (index !== -1) {
        this.conversations.splice(index, 1)
        console.log('🔄 [Store.addSendingMessage] 从原位置移除会话，索引:', index)
      }
      
      // 添加到最前面，确保有用户名和头像
      const conversationData: ConversationVO = {
        userId: toUserId,
        username: this.currentConversation?.username || '未知用户',
        avatar: this.currentConversation?.avatar || '',
        lastMessage: content,
        lastTime: new Date().toISOString(),
        unreadCount: 0
      }
      
      this.conversations.unshift(conversationData)
      console.log('✅ [Store.addSendingMessage] 会话列表已更新，当前会话数:', this.conversations.length)
      
      return tempMessage
    },

    /** 
     * 确认发送的消息（后端推送回来的）
     * 找到本地临时消息并更新为真实数据
     */
    confirmSentMessage(realMessage: Message): void {
      console.log('🔵 [Store.confirmSentMessage] 开始确认消息:', {
        realMessageId: realMessage.id,
        content: realMessage.content,
        toUserId: realMessage.toUserId,
        isSelf: realMessage.isSelf,
        createTime: realMessage.createTime
      })
      
      // 🔥 关键：通过 _sending 状态 + isSelf 匹配（不需要_tempId）
      const tempIndex = this.messages.findIndex(m => 
        m._sending === true && 
        m.isSelf === true &&
        m.content === realMessage.content &&
        m.toUserId === realMessage.toUserId &&
        Math.abs(new Date(m.createTime).getTime() - new Date(realMessage.createTime).getTime()) < 5000 // 5 秒误差内
      )
      
      console.log('🔍 [Store.confirmSentMessage] 查找临时消息，匹配条件:', {
        _sending: true,
        isSelf: true,
        content: realMessage.content,
        toUserId: realMessage.toUserId,
        foundIndex: tempIndex
      })
      
      if (tempIndex !== -1) {
        const oldTempMessage = this.messages[tempIndex]
        console.log('✅ [Store.confirmSentMessage] 找到临时消息:', oldTempMessage)
        
        // 🔥 替换临时消息为真实消息
        this.messages[tempIndex] = {
          ...this.messages[tempIndex],
          id: realMessage.id,
          _tempId: undefined,
          _sending: false,
          status: realMessage.status,
          createTime: realMessage.createTime,
          isSelf: true
        }
        
        console.log('✅ [Store.confirmSentMessage] 临时消息已替换为真实消息:', this.messages[tempIndex])
        console.log('📊 [Store.confirmSentMessage] 当前消息总数:', this.messages.length)
      } else {
        // 没有找到临时消息，可能是网络延迟或页面刷新后
        // 但这条消息确实是自己发的，直接添加（去重）
        const exists = this.messages.some(m => m.id === realMessage.id)
        console.log('⚠️ [Store.confirmSentMessage] 未找到临时消息，检查是否已存在:', {
          exists,
          realMessageId: realMessage.id
        })
        
        if (!exists) {
          this.messages.push({
            ...realMessage,
            _sending: false,
            isSelf: true
          })
          console.log('✅ [Store.confirmSentMessage] 直接添加消息，当前消息总数:', this.messages.length)
        } else {
          console.log('⚠️ [Store.confirmSentMessage] 消息已存在，跳过添加:', realMessage.id)
        }
      }
      
      // 更新会话列表的 lastMessage
      const convIndex = this.conversations.findIndex((c: ConversationVO) => c.userId === realMessage.toUserId)
      if (convIndex !== -1) {
        this.conversations[convIndex].lastMessage = realMessage.content
        this.conversations[convIndex].lastTime = realMessage.createTime
        console.log('✅ [Store.confirmSentMessage] 会话列表已更新')
      }
    },

    /** 加载会话列表 */
    async loadConversations() {
// console.log('🔵 [chatStore] 开始加载会话列表')
      try {
        this.loading = true
        const data = await messageService.getConversations()
// console.log('🔵 [chatStore] API 返回数据:', data)
        
        // 🔥 保留已有的临时会话（通过路由参数创建的）
        const temporaryConversations = this.conversations.filter((c: any) => c._isTemporary)
// console.log('🔵 [chatStore] 保留的临时会话:', temporaryConversations)
        
        // 确保 data 是数组类型，并合并临时会话
        const apiConversations = Array.isArray(data) ? data : []
        this.conversations = [...apiConversations, ...temporaryConversations]
// console.log('🔵 [chatStore] 处理后会话列表:', this.conversations)
        
        // 计算总未读数
        this.unreadCount = this.conversations.reduce((sum: number, conv: ConversationVO) => sum + (conv.unreadCount || 0), 0)
// console.log('🔵 [chatStore] 未读消息数:', this.unreadCount)
      } catch (error) {
        console.error('❌ [chatStore] 加载会话列表失败:', error)
        this.conversations = []
        this.unreadCount = 0
        throw error
      } finally {
        this.loading = false
// console.log('🔵 [chatStore] 加载完成，loading=false')
      }
    },

    /** 切换当前聊天对象 */
    async switchConversation(conversation: ConversationVO & { userId?: number }) {
// console.log('🔵 [chatStore] 切换会话:', conversation)
      try {
        if (!conversation || !(conversation as any).userId) {
          console.error('❌ [chatStore] 无效的会话对象')
          return
        }
        
        this.currentConversation = conversation
// console.log('🔵 [chatStore] currentConversation 已设置:', this.currentConversation)
        this.messagePage = 1
        this.hasMore = true
        
        // 🔥 如果当前会话没有用户名或头像，尝试从服务器获取
        if ((!conversation.username || conversation.username === '未知用户' || !conversation.avatar)) {
// console.log('🔵 [chatStore] 检测到会话信息不完整，尝试获取用户信息')
          try {
            const userInfo = await userService.getUserById((conversation as any).userId)
            if (userInfo) {
              conversation.username = userInfo.nickname || userInfo.account
              conversation.avatar = userInfo.avatar || ''
              
              // 更新 currentConversation
              this.currentConversation = { ...this.currentConversation, ...conversation }
              
              // 更新会话列表中的信息
              const index = this.conversations.findIndex((c: any) => c.userId === (conversation as any).userId)
              if (index !== -1) {
                this.conversations[index].username = conversation.username
                this.conversations[index].avatar = conversation.avatar
              }
// console.log('🔵 [chatStore] 用户信息已更新:', conversation.username, conversation.avatar)
            }
          } catch (error) {
            console.error('❌ [chatStore] 获取用户信息失败:', error)
          }
        }
        
        // 加载聊天记录
// console.log('🔵 [chatStore] 开始加载聊天记录，userId:', (conversation as any).userId)
        await this.loadMessages((conversation as any).userId, true)
// console.log('🔵 [chatStore] 聊天记录加载完成，消息数:', this.messages.length)
        
        // 标记为已读
        if ((conversation as any).unreadCount && (conversation as any).unreadCount > 0) {
// console.log('🔵 [chatStore] 标记为已读，fromUserId:', (conversation as any).userId)
          await messageService.markAsRead((conversation as any).userId)
          // 更新本地未读数
          this.unreadCount -= (conversation as any).unreadCount
          ;(conversation as any).unreadCount = 0
          
          // 更新会话列表中的未读数
          const index = this.conversations.findIndex((c: any) => c.userId === (conversation as any).userId)
          if (index !== -1) {
            this.conversations[index].unreadCount = 0
          }
        }
      } catch (error) {
        console.error('❌ [chatStore] 切换会话失败:', error)
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

    /** 添加发送的消息（旧方法，保留兼容性） */
    addSentMessage(message: Message) {
      // 🔥 如果消息带有_tempId，说明是乐观更新的确认
      if (message._tempId) {
        this.confirmSentMessage(message)
        return
      }
      
      // 否则使用原有逻辑
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
      console.log('🔵 [Store.receiveMessage] 收到新消息:', {
        messageId: message.id,
        content: message.content,
        fromUserId: message.fromUserId,
        toUserId: message.toUserId,
        isSelf: message.isSelf
      })
      
      // 🔥 如果是自己发送的消息（isSelf=true），跳过，避免重复处理
      // （因为自己发送的消息已经在 HTTP 响应时添加到 store 了）
      if (message.isSelf) {
        console.log('⚠️ [Store.receiveMessage] 检测到 isSelf=true，跳过避免重复添加')
        return
      }
      
      // 如果当前正在和该用户聊天，直接添加到消息列表
      if (this.currentConversation && 
          (message.fromUserId === this.currentConversation.userId || 
           message.toUserId === this.currentConversation.userId)) {
        console.log('✅ [Store.receiveMessage] 当前正在与该用户聊天，添加到消息列表')
        this.messages.push(message)
        console.log('📊 [Store.receiveMessage] 添加后消息总数:', this.messages.length)
        
        // 更新会话列表
        const index = this.conversations.findIndex((c: ConversationVO) => c.userId === message.fromUserId)
        if (index !== -1) {
          this.conversations[index].lastMessage = message.content
          this.conversations[index].lastTime = message.createTime
          
          // 如果当前窗口打开，标记为已读
          if (this.currentConversation.userId === message.fromUserId) {
            this.conversations[index].unreadCount = 0
            console.log('✅ [Store.receiveMessage] 当前窗口打开，标记为已读')
          } else {
            this.conversations[index].unreadCount++
            this.unreadCount++
            console.log('✅ [Store.receiveMessage] 增加未读数:', this.unreadCount)
          }
          
          // 移动到最前面
          const conv = this.conversations.splice(index, 1)[0]
          this.conversations.unshift(conv)
          console.log('✅ [Store.receiveMessage] 会话已移动到顶部')
        }
      } else {
        console.log('⚠️ [Store.receiveMessage] 当前未与该用户聊天，只更新会话列表')
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
          console.log('✅ [Store.receiveMessage] 会话已更新并移动到顶部')
        } else {
          console.log('⚠️ [Store.receiveMessage] 未找到对应会话')
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
