
import { ElMessage } from 'element-plus'
import type {ConversationVO, Message} from '@/types/message'
import messageService from '@/api/message'
import userService from '@/api/user'
import { getWebSocket } from '@/utils/websocket'
import {defineStore} from "pinia";

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

    /** 
     *  更新消息状态（用于已读回执）
     * @param data 已读回执数据 { fromUserId, toUserId }
     */
    updateMessageStatus(data: { fromUserId: number, toUserId: number }) {
      //  根据 fromUserId 和 toUserId 找到对应的消息
      const messagesToUpdate = this.messages.filter((m: Message) => 
        m.fromUserId === data.fromUserId && 
        m.toUserId === data.toUserId &&
        (m.status === 0 || m.status === 'SENT' || m.status === 'DELIVERED')
      )

      
      
      
      messagesToUpdate.forEach((message: Message) => {
        message.status = 'READ'
        
      })
      
      
    },

    /** 
     * 乐观添加发送中的消息（关键方法！）
     * @returns 临时消息对象，包含_tempId
     */
    addSendingMessage(content: string, toUserId: number): Message {
      const tempId = `temp_${Date.now()}_${Math.random().toString(36).  substr(2, 9)}`
      
      //  从 localStorage 获取当前用户 ID
      const useridStr = localStorage.getItem('userid')
      const currentUserId = Number(useridStr || '0')
      
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
     
      // 更新会话列表
      const index = this.conversations.findIndex((c: ConversationVO) => c.userId === toUserId)
      if (index !== -1) {
        this.conversations.splice(index, 1)

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

      
      return tempMessage
    },

    /** 
     * 确认发送的消息（后端推送回来的）
     * 找到本地临时消息并更新为真实数据
     */
    confirmSentMessage(realMessage: Message): void {

//         realMessageId: realMessage.id,
//         content: realMessage.content,
//         toUserId: realMessage.toUserId,
//         isSelf: realMessage.isSelf,
//         createTime: realMessage.createTime
//       })
      
      //  关键：通过 _sending 状态 + isSelf 匹配（不需要_tempId）
      const tempIndex = this.messages.findIndex((m: Message) =>
        m._sending === true && 
        m.isSelf === true &&
        m.content === realMessage.content &&
        m.toUserId === realMessage.toUserId &&
        Math.abs(new Date(m.createTime).getTime() - new Date(realMessage.createTime).getTime()) < 5000 // 5 秒误差内
      )


      
      if (tempIndex !== -1) {
        const oldTempMessage = this.messages[tempIndex]
        
        //  关键修改：保留完整的消息结构，包括 fromUser 和 toUser
        this.messages[tempIndex] = {
          ...realMessage,  //  使用真实消息的完整数据
          _tempId: undefined,
          _sending: false,
          isSelf: true
        }


      } else {
        // 没有找到临时消息，可能是网络延迟或页面刷新后
        // 但这条消息确实是自己发的，直接添加（去重）
        const exists = this.messages.some((m: Message) => m.id === realMessage.id)
        if (!exists) {
          this.messages.push({
            ...realMessage,
            _sending: false,
            isSelf: true
          })

        } else {

        }
      }
      
      // 更新会话列表的 lastMessage
      const convIndex = this.conversations.findIndex((c: ConversationVO) => c.userId === realMessage.toUserId)
      if (convIndex !== -1) {
        this.conversations[convIndex].lastMessage = realMessage.content
        this.conversations[convIndex].lastTime = realMessage.createTime

      }
    },

    /** 加载会话列表 */
    async loadConversations() {

      try {
        this.loading = true
        const data = await messageService.getConversations()

        
        //  保留已有的临时会话（通过路由参数创建的）
        const temporaryConversations = this.conversations.filter((c: any) => c._isTemporary)

        
        // 确保 data 是数组类型，并合并临时会话
        const apiConversations = Array.isArray(data) ? data : []
        this.conversations = [...apiConversations, ...temporaryConversations]

        
        // 计算总未读数
        this.unreadCount = this.conversations.reduce((sum: number, conv: ConversationVO) => sum + (conv.unreadCount || 0), 0)

      } catch (error) {
        console.error('❌ [chatStore] 加载会话列表失败:', error)
        this.conversations = []
        this.unreadCount = 0
        throw error
      } finally {
        this.loading = false

      }
    },

    /** 切换当前聊天对象 */
    async switchConversation(conversation: ConversationVO & { userId?: number }) {

      try {
        if (!conversation || !(conversation as any).userId) {
          console.error('❌ [chatStore] 无效的会话对象')
          return
        }
        
        this.currentConversation = conversation

        this.messagePage = 1
        this.hasMore = true
        
        //  如果当前会话没有用户名或头像，尝试从服务器获取
        if ((!conversation.username || conversation.username === '未知用户' || !conversation.avatar)) {

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

            }
          } catch (error) {
            console.error('❌ [chatStore] 获取用户信息失败:', error)
          }
        }
        
        // 加载聊天记录

        await this.loadMessages((conversation as any).userId, true)

        
        //  标记为已读（使用 WebSocket 发送已读回执）
        if ((conversation as any).unreadCount && (conversation as any).unreadCount > 0) {

          
          //  使用 WebSocket 发送已读回执（代替 HTTP 接口）
          const ws = getWebSocket()
          if (ws && ws.isConnected()) {

            ws.sendReadReceipt((conversation as any).userId)
          } else {
            console.warn('⚠️ [chatStore] WebSocket 未连接，无法发送已读回执')
          }
          
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
      //  如果消息带有_tempId，说明是乐观更新的确认
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

//         messageId: message.id,
//         content: message.content,
//         fromUserId: message.fromUserId,
//         toUserId: message.toUserId,
//         isSelf: message.isSelf
//       })
      
      //  如果是自己发送的消息（isSelf=true），跳过，避免重复处理
      // （因为自己发送的消息已经在 HTTP 响应时添加到 store 了）
      if (message.isSelf) {

        return
      }
      
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

        } else {

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
    },

    /** 
     *  乐观更新：撤回消息
     * 立即将消息标记为已撤回状态
     */
    recallMessageOptimistic(messageId: number): void {

      
      const msgIndex = this.messages.findIndex((m: Message) => m.id === messageId)
      if (msgIndex !== -1) {
        const msg = this.messages[msgIndex]

        
        //  直接修改原对象，触发响应式更新
        this.messages[msgIndex] = {
          ...msg,
          content: 'recall',
          isRecalled: true,
          _isSystemTip: true,
          _tipType: 'recall',
          _tipUsername: msg.fromUser?.username || msg.fromUser?.nickname || ''
        } as any

      } else {
        console.warn('⚠️ [Store.recallMessageOptimistic] 未找到消息:', messageId)
      }
    },

    /** 
     *  乐观删除：立即从消息列表中移除（仅对自己可见）
     * @param messageId 要删除的消息 ID
     * @returns 被删除的消息对象（用于可能的回滚），如果未找到则返回 null
     */
    deleteMessageOptimistic(messageId: number): Message | null {

      
      const msgIndex = this.messages.findIndex((m: Message) => m.id === messageId)
      if (msgIndex !== -1) {
        const deletedMsg = this.messages[msgIndex]

        
        //  从数组中移除（使用 splice 确保响应式更新）
        this.messages.splice(msgIndex, 1)

        
        //  返回被删除的消息（用于可能的回滚）
        return deletedMsg
      } else {
        console.warn('⚠️ [Store.deleteMessageOptimistic] 未找到消息:', messageId)
        return null
      }
    },

    /** 
     *  接收撤回通知（WebSocket 推送）
     * 处理后端推送的撤回消息
     */
    receiveRecallNotification(data: { messageId: number, userId: number, reason?: string }): void {

      
      const msgIndex = this.messages.findIndex((m: Message) => m.id === data.messageId)
      if (msgIndex !== -1) {
        const msg = this.messages[msgIndex]

        
        //  转换为系统提示消息
        this.messages[msgIndex] = {
          ...msg,
          content: 'recall',
          isRecalled: true,
          _isSystemTip: true,
          _tipType: 'recall',
          _tipUsername: msg.fromUser?.username || msg.fromUser?.nickname || ''
        } as any

      } else {
        console.warn('⚠️ [Store.receiveRecallNotification] 未找到消息:', data.messageId)
      }
    },

    /** 
     *  接收删除通知（WebSocket 推送）
     * 处理后端推送的删除消息（确认删除）
     * 
     * 注意：由于是单向删除，对方不会收到删除通知
     * 这个方法主要用于：
     * 1. 确认自己的删除操作成功
     * 2. 处理异常情况（如乐观删除失败）
     */
    receiveDeleteNotification(data: { messageId: number, userId: number, bothDeleted?: boolean }): void {

      
      //  检查消息是否已经被乐观删除
      const msgIndex = this.messages.findIndex((m: Message) => m.id === data.messageId)
      
      if (msgIndex !== -1) {
        // 消息还在列表中，说明乐观删除可能失败了，现在执行删除

        this.messages.splice(msgIndex, 1)

      } else {
        // 消息已经不在了，说明乐观删除成功了

      }
    },

    /**
     *  保存当前会话到 sessionStorage（用于页面刷新后恢复）
     */
    saveCurrentSessionToStorage(): void {
      if (this.currentConversation) {
        const sessionData = {
          userId: this.currentConversation.userId,
          username: this.currentConversation.username,
          avatar: this.currentConversation.avatar,
          timestamp: Date.now()
        }
        sessionStorage.setItem('lastChatSession', JSON.stringify(sessionData))

      }
    },

    /**
     *  从 sessionStorage 恢复会话信息
     * @returns 会话信息，如果没有则返回 null
     */
    restoreSessionFromStorage(): { userId: number, username: string, avatar: string } | null {
      const sessionStr = sessionStorage.getItem('lastChatSession')
      if (!sessionStr) {

        return null
      }
      
      try {
        const sessionData = JSON.parse(sessionStr)
        
        // 检查是否过期（24 小时内有效）
        const now = Date.now()
        const expiryTime = 24 * 60 * 60 * 1000 // 24 小时
        if (now - sessionData.timestamp > expiryTime) {

          sessionStorage.removeItem('lastChatSession')
          return null
        }

        return {
          userId: Number(sessionData.userId),
          username: sessionData.username,
          avatar: sessionData.avatar
        }
      } catch (error) {
        console.error('❌ [Store] 解析会话信息失败:', error)
        sessionStorage.removeItem('lastChatSession')
        return null
      }
    },

    /**
     *  清除保存的会话信息
     */
    clearSavedSession(): void {
      sessionStorage.removeItem('lastChatSession')

    }

  }
})
