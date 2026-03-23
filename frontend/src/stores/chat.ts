
import { ElMessage } from 'element-plus'
import type {ConversationVO, Message} from '@/types/message'
import messageService from '@/api/message'
import userService from '@/api/user'
import { getWebSocket } from '@/utils/websocket'
import {defineStore} from "pinia";
import { MessageStatus, isUnreadMessage, isReadMessage } from '@/types/message'
import type { WsUserOnlineStatus } from '@/types/message'

interface ChatState {
  conversations: ConversationVO[]
  currentConversation: ConversationVO | null
  messages: Message[]
  unreadCount: number
  loading: boolean
  messagePage: number
  hasMore: true
  // 在线状态管理
  onlineUsers: Map<number, { online: boolean; lastSeenAt?: string }>
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    conversations: [],
    currentConversation: null,
    messages: [],
    unreadCount: 0,
    loading: false,
    messagePage: 1,
    hasMore: true,
    onlineUsers: new Map()
  }),

  getters: {
    /** 获取当前聊天对象的消息列表 */
    currentMessages: (state): Message[] => {
      return state.messages
    },

    /** 检查是否有未读消息 */
    hasUnreadMessages: (state): boolean => {
      return state.unreadCount > 0
    },

    /** 获取指定用户的在线状态 */
    getUserOnlineStatus: (state) => {
      return (userId: number) => {
        return state.onlineUsers.get(userId) || { online: false }
      }
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
     * @param data 已读回执数据 { fromUserId: 阅读者，toUserId: 消息发送者 }
     */
    updateMessageStatus(data: { fromUserId: number, toUserId: number }) {
        const readerUserId = data.fromUserId
        const senderUserId = data.toUserId

        
        const messagesToUpdate = this.messages.filter((m: Message) => {
            const isSentToReader = m.toUserId === readerUserId
            const isFromSender = m.fromUserId === senderUserId
            const isUnread = m.status !== undefined && isUnreadMessage(m.status)
            
            const shouldUpdate = isSentToReader && isFromSender && isUnread
            
            if (isSentToReader && isFromSender) {

            }
            
            return shouldUpdate
        })

        
        if (messagesToUpdate.length === 0) {
            //  添加：检查是否有已经是已读的消息
            const alreadyReadMessages = this.messages.filter((m: Message) => 
                m.toUserId === readerUserId && 
                m.fromUserId === senderUserId &&
                isReadMessage(m.status)
            )
            
            if (alreadyReadMessages.length > 0) {

            } else {
                console.warn('⚠️ [Store.updateMessageStatus] 没有找到匹配的消息')
            }
            return
        }

        messagesToUpdate.forEach((message: Message) => {
            const oldStatus = message.status
            message.status = MessageStatus.READ

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



      // 关键修复：比较时需要统一格式，都转换为字符串进行比较
      const normalizeContent = (content: any): string => {
        return typeof content === 'string' ? content : JSON.stringify(content)
      }

      const tempIndex = this.messages.findIndex((m: Message) => {
        const isSending = m._sending === true
        const isSelf = m.isSelf === true
        const toUserMatch = m.toUserId === realMessage.toUserId
        const contentMatch = normalizeContent(m.content) === normalizeContent(realMessage.content)
        const timeDiff = Math.abs(new Date(m.createTime).getTime() - new Date(realMessage.createTime).getTime())
        const timeMatch = timeDiff < 5000

        
        return isSending && isSelf && toUserMatch && contentMatch && timeMatch
      })


      if (tempIndex !== -1) {

        
        const oldTempMessage = this.messages[tempIndex]


        
        // 关键修改：保留完整的消息结构，包括 fromUser 和 toUser
        this.messages[tempIndex] = {
          ...realMessage,  // 使用真实消息的完整数据
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

      // 更新会话列表
      const index = this.conversations.findIndex((c: ConversationVO) => c.userId === realMessage.toUserId)
      if (index !== -1) {
        this.conversations.splice(index, 1)
      }
      
      // 添加到最前面，确保有用户名和头像
      const conversationData: ConversationVO = {
        userId: realMessage.toUserId,
        username: this.currentConversation?.username || '未知用户',
        avatar: this.currentConversation?.avatar || '',
        lastMessage: typeof realMessage.content === 'string' ? realMessage.content : JSON.stringify(realMessage.content),
        lastTime: realMessage.createTime,
        unreadCount: 0
      }
      
      this.conversations.unshift(conversationData)

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
            const ws = getWebSocket()
            if (ws && ws.isConnected()) {
                ws.sendReadReceipt((conversation as any).userId)
            } else {
                console.warn('⚠️ [chatStore] WebSocket 未连接，无法发送已读回执')
            }
            
            this.unreadCount -= (conversation as any).unreadCount
            ;(conversation as any).unreadCount = 0
            
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
        if (message.isSelf) {
            return
        }
        
        if (this.currentConversation && 
            (message.fromUserId === this.currentConversation.userId || 
             message.toUserId === this.currentConversation.userId)) {
            
            this.messages.push(message)
            
            const index = this.conversations.findIndex((c: ConversationVO) => c.userId === message.fromUserId)
            if (index !== -1) {
                this.conversations[index].lastMessage = message.content
                this.conversations[index].lastTime = message.createTime
                
                if (this.currentConversation.userId === message.fromUserId) {
                    //  关键修改：只更新未读数，不发送已读回执
                    // 已读回执由 ChatDetail 组件统一处理
                    this.conversations[index].unreadCount = 0

                } else {
                    this.conversations[index].unreadCount++
                    this.unreadCount++
                }
                
                const conv = this.conversations.splice(index, 1)[0]
                this.conversations.unshift(conv)
            }
        } else {
            const index = this.conversations.findIndex((c: ConversationVO) => c.userId === message.fromUserId)
            if (index !== -1) {
                this.conversations[index].lastMessage = message.content
                this.conversations[index].lastTime = message.createTime
                this.conversations[index].unreadCount++
                this.unreadCount++
                
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

    },

    /** 初始化在线状态监听 */
    initializeOnlineStatus() {
      const ws = getWebSocket()
      if (!ws || !ws.isConnected()) {
        console.warn('⚠️ [ChatStore] WebSocket 未连接，无法初始化在线状态')
        return
      }

      // 订阅用户在线状态更新
      ws.on('USER_ONLINE_STATUS', (data: any) => {
        
        // 兼容处理：后端可能返回数组或单个对象
        let statusData
        if (Array.isArray(data)) {
          // 如果是数组，遍历处理所有用户
          data.forEach((item: any) => {
            this.updateUserOnlineStatus(item.userId, {
              online: item.isOnline ?? item.online,
              lastSeenAt: item.lastSeenAt
            })
          })
          return
        } else {
          statusData = data
        }

        const current = this.onlineUsers.get(statusData.userId)
        this.onlineUsers.set(statusData.userId, {
          online: statusData.isOnline ?? statusData.online,
          lastSeenAt: statusData.lastSeenAt
        })

        // 如果是在线状态，可选：订阅好友列表
        if ((statusData.isOnline ?? statusData.online) && !current?.online) {

        }
      })

      // 订阅在线用户列表（批量更新）
      ws.on('USER_LIST_UPDATE', (data: any) => {

        
        data.users.forEach((user: any) => {
          this.onlineUsers.set(user.userId, {
            online: user.online,
            lastSeenAt: user.lastSeenAt
          })
        })
      })

      // 订阅所有好友的在线状态
      ws.subscribeFriendsOnlineStatus()
    },

    /** 更新用户在线状态 */
    updateUserOnlineStatus(userId: number, status: { online: boolean; lastSeenAt?: string }) {
      this.onlineUsers.set(userId, status)
    },

    /** 检查用户是否在线 */
    isUserOnline(userId: number): boolean {
      return this.onlineUsers.get(userId)?.online ?? false
    }

  }
})
