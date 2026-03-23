// src/stores/circleChat.ts
import { defineStore } from 'pinia'
import type { Circle, CircleConversation, CircleMessage, CircleMember, CircleRole } from '@/types/circleChat'
import type { MessageStatus } from '@/types/message'
import circleChatMessageService from '@/service/circleChatMessageService'
import { wsLogger } from '@/utils/websocketLogger'
import { getWebSocket } from '@/utils/websocket'

interface CircleChatState {
  // 圈子列表
  circles: Circle[]
  
  // 当前选中的圈子
  currentCircle: Circle | null
  
  // 圈子会话列表
  conversations: CircleConversation[]
  
  // 当前圈子的消息列表
  messages: CircleMessage[]
  
  // 当前圈子的成员列表
  members: CircleMember[]
  
  // 加载状态
  loading: boolean
  
  // 消息分页
  messagePage: number
  hasMore: boolean
  
  // WebSocket 连接状态
  isConnected: boolean
  
  // 在线成员 Map
  onlineMembers: Map<number, boolean>
}

export const useCircleChatStore = defineStore('circleChat', {
  state: (): CircleChatState => ({
    circles: [],
    currentCircle: null,
    conversations: [],
    messages: [],
    members: [],
    loading: false,
    messagePage: 1,
    hasMore: true,
    isConnected: false,
    onlineMembers: new Map()
  }),

  getters: {
    /** 获取当前圈子的消息列表 */
    currentMessages: (state): CircleMessage[] => {
      return state.messages
    },

    /** 检查是否有未读消息 */
    hasUnreadMessages: (state): boolean => {
      return state.conversations.some((conv: CircleConversation) => conv.unreadCount > 0)
    },

    /** 获取总未读数 */
    totalUnreadCount: (state): number => {
      return state.conversations.reduce((sum: number, conv: CircleConversation) => sum + conv.unreadCount, 0)
    },

    /** 获取成员在线状态 */
    getMemberOnlineStatus: (state) => {
      return (userId: number) => {
        return state.onlineMembers.get(userId) || false
      }
    }
  },

  actions: {
    /** 初始化圈子聊天状态 */
    initializeState() {
      this.circles = []
      this.currentCircle = null
      this.conversations = []
      this.messages = []
      this.members = []
      this.loading = false
      this.messagePage = 1
      this.hasMore = true
    },

    /** 初始化圈子消息监听 */
    initializeCircleMessages() {
      const ws = getWebSocket()
      if (!ws || !ws.isConnected()) {
        wsLogger.warn('WebSocket 未连接，无法初始化圈子消息')
        return
      }

      wsLogger.info('初始化圈子消息服务')

      // 使用新的圈子消息服务
      circleChatMessageService.init()
      
      // 订阅圈子消息
      circleChatMessageService.onMessage((message) => {
        this.receiveMessage(message)
      })
      
      // 订阅消息删除
      circleChatMessageService.onDelete((data) => {
        wsLogger.debug('收到圈子消息删除通知', data)
        this.handleDeleteMessageNotification(data.messageId, data.deleterId, data.deleterNickname)
      })
    },

    /** 更新圈子会话列表 */
    setConversations(conversations: CircleConversation[]) {
      this.conversations = conversations
    },

    /** 添加或更新会话 */
    upsertConversation(conversation: CircleConversation) {
      const index = this.conversations.findIndex((c: CircleConversation) => c.circleId === conversation.circleId)
      if (index !== -1) {
        this.conversations[index] = conversation
      } else {
        this.conversations.unshift(conversation)
      }
    },

    /** 设置当前圈子 */
    setCurrentCircle(circle: Circle | null) {
      this.currentCircle = circle
    },

    /** 设置消息列表 */
    setMessages(messages: CircleMessage[], reset = false) {
      if (reset) {
        this.messages = messages
      } else {
        // 加载更多时，添加到前面
        this.messages = [...messages, ...this.messages]
      }
      this.hasMore = messages.length === 20
      this.messagePage++
    },

    /** 乐观添加：正在发送的消息 */
    addSendingMessage(content: string, circleId: number): CircleMessage {
      // 修复：直接从 localStorage 获取当前用户 ID
      const currentUserId = parseInt(localStorage.getItem('userid') || '0')
      
      const tempId = `temp_${Date.now()}_${Math.random()}`
      
      // 获取当前用户信息（用于显示头像和昵称）
      const currentUserInfo = {
        id: currentUserId,
        nickname: localStorage.getItem('nickname') || '我',
        avatar: localStorage.getItem('avatar') || ''
      }
      
      const tempMessage: CircleMessage = {
        id: 0,
        _tempId: tempId,
        _sending: true,
        circleId: circleId,
        senderId: currentUserId,
        sender: {  // 添加发送者信息，确保头像能显示
          id: currentUserId,
          nickname: currentUserInfo.nickname,
          avatar: currentUserInfo.avatar
        },
        content: content,
        msgType: 0,
        status: 'SENDING',
        createTime: new Date().toISOString(),
        isSelf: true,
        action: 'SEND'
      }
      
      this.messages.push(tempMessage)
      
      // 更新会话列表
      const index = this.conversations.findIndex((c: CircleConversation) => c.circleId === circleId)
      if (index !== -1) {
        this.conversations[index].lastMessageContent = content
        this.conversations[index].lastMessageTime = new Date().toISOString()
        const conv = this.conversations.splice(index, 1)[0]
        this.conversations.unshift(conv)
      }
      
      return tempMessage
    },

    /** 确认发送的消息 */
    confirmSentMessage(realMessage: CircleMessage): void {
      
      // 关键修复：比较时需要统一格式，都转换为字符串进行比较
      const normalizeContent = (content: any): string => {
        return typeof content === 'string' ? content : JSON.stringify(content)
      }
      
      const tempIndex = this.messages.findIndex((m: CircleMessage) => {
        const isSending = m._sending === true
        const isSelf = m.isSelf === true
        const contentMatch = normalizeContent(m.content) === normalizeContent(realMessage.content)
        const timeDiff = Math.abs(new Date(m.createTime).getTime() - new Date(realMessage.createTime).getTime())
        const timeMatch = timeDiff < 5000
        
        return isSending && isSelf && contentMatch && timeMatch
      })

      if (tempIndex !== -1) {
        
        this.messages[tempIndex] = {
          ...realMessage,
          _tempId: undefined,
          _sending: false,
          isSelf: true
        }

      } else {
        const exists = this.messages.some((m: CircleMessage) => m.id === realMessage.id)
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
      const convIndex = this.conversations.findIndex((c: CircleConversation) => c.circleId === realMessage.circleId)
      if (convIndex !== -1) {
        this.conversations[convIndex].lastMessageContent = realMessage.content
        this.conversations[convIndex].lastMessageTime = realMessage.createTime
        
        // 移到最前面
        const conv = this.conversations.splice(convIndex, 1)[0]
        this.conversations.unshift(conv)
      }
    },

    /** 接收新消息（WebSocket） */
    receiveMessage(message: CircleMessage) {
      // 关键修改：不再检查 message.isSelf，由组件层处理
      
      wsLogger.debug('收到圈子消息', {
        circleId: message.circleId,
        senderId: message.senderId,
        currentCircleId: this.currentCircle?.id,
        contentPreview: typeof message.content === 'string' ? message.content.substring(0, 30) : '[object]'
      })
      
      if (this.currentCircle && message.circleId === this.currentCircle.id) {
        // 当前正在查看这个圈子，直接添加消息，不增加未读数
        this.messages.push(message)
        
        // 清零未读数（本地）
        const convIndex = this.conversations.findIndex((c: CircleConversation) => c.circleId === message.circleId)
        if (convIndex !== -1) {
          this.conversations[convIndex].unreadCount = 0
        }
        
        // 关键修复：立即调用后端 API 标记为已读（更新数据库的 last_read_time）
        // 使用防抖，避免频繁调用
        this.debounceMarkAsRead(message.circleId)
        
        wsLogger.debug('当前圈子消息，已清零未读数', { circleId: message.circleId })
      } else {
        // 不在当前圈子，增加未读数
        const convIndex = this.conversations.findIndex((c: CircleConversation) => c.circleId === message.circleId)
        if (convIndex !== -1) {
          this.conversations[convIndex].lastMessageContent = message.content
          this.conversations[convIndex].lastMessageTime = message.createTime
          this.conversations[convIndex].unreadCount++
          const conv = this.conversations.splice(convIndex, 1)[0]
          this.conversations.unshift(conv)
          
          wsLogger.debug('非当前圈子消息，增加未读数', {
            circleId: message.circleId,
            newUnreadCount: this.conversations[convIndex].unreadCount
          })
        } else {
          // 如果会话列表中还没有这个圈子，创建新的会话项
          const newConv: CircleConversation = {
            circleId: message.circleId,
            circleName: '未知圈子',
            lastMessageContent: message.content,
            lastMessageSenderId: message.senderId,
            lastMessageSenderNickname: message.sender?.nickname,
            lastMessageTime: message.createTime,
            unreadCount: 1,
            memberCount: 0
          }
          this.conversations.unshift(newConv)
          
          wsLogger.debug('新圈子消息，创建新会话', { circleId: message.circleId })
        }
      }
    },

    /** 撤回消息（乐观更新） */
    recallMessageOptimistic(messageId: number, reason?: string): void {
      const msgIndex = this.messages.findIndex((m: CircleMessage) => m.id === messageId)
      if (msgIndex !== -1) {
        const msg = this.messages[msgIndex]
        this.messages[msgIndex] = {
          ...msg,
          content: '',
          isRecalled: true,
          _isSystemTip: true,
          _tipType: 'recall',
          _tipUsername: msg.sender?.nickname || ''
        }
      }
    },

    /** 删除消息（乐观更新，仅管理员） */
    deleteMessageOptimistic(messageId: number, deletedByUserId: number, deletedByUsername?: string): void {
      const msgIndex = this.messages.findIndex((m: CircleMessage) => m.id === messageId)
      if (msgIndex !== -1) {
        const msg = this.messages[msgIndex]
        
        // 1. 将原消息标记为已删除
        this.messages[msgIndex] = {
          ...msg,
          isDeleted: true,
          content: '',
          _isSystemTip: true,
          _tipType: 'delete',
          _tipUsername: deletedByUsername || '管理员'
        }

      } else {
        console.warn('⚠️ [CircleChat] 未找到要删除的消息:', messageId)
      }
    },

    /** 处理收到的消息删除通知（WebSocket 推送） */
    handleDeleteMessageNotification(messageId: number, deleterId: number, deleterNickname?: string): void {
      const msgIndex = this.messages.findIndex((m: CircleMessage) => m.id === messageId)
      if (msgIndex !== -1) {
        const msg = this.messages[msgIndex]
        const senderNickname = msg.sender?.nickname || '未知用户'
        
        // 创建系统提示消息：某人的消息被管理员某某删除了
        this.messages.splice(msgIndex, 1, {
          ...msg,
          id: messageId,
          content: '',
          deletedByAdmin: true,
          _isSystemTip: true,
          _tipType: 'delete',
          _tipUsername: deleterNickname || '管理员',
          _deleteDetail: `${senderNickname}的消息被${deleterNickname || '管理员'}删除`
        })

      } else {
        console.warn('⚠️ [CircleChat] 未找到要处理删除通知的消息:', messageId)
      }
    },

    /** 切换当前圈子 */
    async switchCircle(circle: Circle) {
      this.currentCircle = circle
      this.messagePage = 1
      this.hasMore = true
      this.messages = []
      
      // 清零未读数
      const convIndex = this.conversations.findIndex((c: CircleConversation) => c.circleId === circle.id)
      if (convIndex !== -1) {
        this.conversations[convIndex].unreadCount = 0
      }
    },

    /** 设置成员列表 */
    setMembers(members: CircleMember[]) {
      // 添加角色转换逻辑
      const transformedMembers = members.map(member => ({
        ...member,
        role: this.transformRole(member.role)
      }))
      
      this.members = transformedMembers
    },

    /** 
     * 将后端数字角色转换为前端字符串角色
     * @param role 数字角色 (0, 1, 2) 或已转换的角色
     * @returns 字符串角色 ('OWNER', 'ADMIN', 'MEMBER')
     */
    transformRole(role: any): CircleRole {
      if (typeof role === 'string') {
        return role as CircleRole
      }
      
      switch (role) {
        case 2:
          return 'OWNER'
        case 1:
          return 'ADMIN'
        case 0:
          return 'MEMBER'
        default:
          console.warn('未知的角色值:', role)
          return 'MEMBER'
      }
    },

    /** 更新成员在线状态 */
    updateMemberOnlineStatus(userId: number, online: boolean) {
      this.onlineMembers.set(userId, online)
    },

    /** 保存当前圈子到 sessionStorage */
    saveCurrentCircleToStorage(): void {
      if (this.currentCircle) {
        const sessionData = {
          circleId: this.currentCircle.id,
          circleName: this.currentCircle.name,
          circleAvatar: this.currentCircle.avatar,
          timestamp: Date.now()
        }
        sessionStorage.setItem('lastCircleSession', JSON.stringify(sessionData))
      }
    },

    /** 从 sessionStorage 恢复 */
    restoreCircleFromStorage(): { circleId: number, circleName: string, circleAvatar?: string } | null {
      const sessionStr = sessionStorage.getItem('lastCircleSession')
      if (!sessionStr) {
        return null
      }
      
      try {
        const sessionData = JSON.parse(sessionStr)
        const now = Date.now()
        const expiryTime = 24 * 60 * 60 * 1000
        
        if (now - sessionData.timestamp > expiryTime) {
          sessionStorage.removeItem('lastCircleSession')
          return null
        }

        return {
          circleId: Number(sessionData.circleId),
          circleName: sessionData.circleName,
          circleAvatar: sessionData.circleAvatar
        }
      } catch (error) {
        console.error('解析圈子会话失败:', error)
        sessionStorage.removeItem('lastCircleSession')
        return null
      }
    },

    /** 清除保存的会话 */
    clearSavedCircle(): void {
      sessionStorage.removeItem('lastCircleSession')
    },

    /** 防抖：标记圈子为已读（延迟 500ms，避免频繁调用） */
    debounceMarkAsReadTimeouts: new Map<number, NodeJS.Timeout>(),
    
    /** 标记圈子消息为已读（发送已读回执） */
    markCircleAsRead(circleId: number): void {
      const ws = require('@/utils/websocket').getWebSocket()
      if (ws && ws.isConnected()) {
        const client = (ws as any).client
        if (client && client.connected) {
          const readReceipt = {
            circleId: circleId,
            timestamp: Date.now()
          }
          
          client.publish({
            destination: '/app/circle-read-receipt',
            body: JSON.stringify(readReceipt)
          })

        }
      } else {
        console.warn('⚠️ [CircleChat] WebSocket 未连接，无法发送已读回执')
      }
    },

    /** 从后端获取并更新未读消息数 */
    async updateUnreadCountFromServer(circleId: number): Promise<void> {
      try {
        const { circleChatApi } = await import('@/api/circle')
        const count = await circleChatApi.getUnreadCount(circleId)
        
        const convIndex = this.conversations.findIndex((c: CircleConversation) => c.circleId === circleId)
        if (convIndex !== -1) {
          this.conversations[convIndex].unreadCount = count

        }
      } catch (error) {
        console.error('❌ [CircleChat] 获取未读数失败:', error)
      }
    },

    /** 标记圈子为已读（调用后端接口） */
    async markAsReadByAPI(circleId: number): Promise<void> {
      try {
        const { circleChatApi } = await import('@/api/circle')
        await circleChatApi.markAsRead(circleId)
        
        // 更新本地未读数为 0
        const convIndex = this.conversations.findIndex((c: CircleConversation) => c.circleId === circleId)
        if (convIndex !== -1) {
          this.conversations[convIndex].unreadCount = 0
        }

      } catch (error) {
        console.error('❌ [CircleChat] 标记已读失败:', error)
      }
    },

    /** 处理加载的消息中的撤回消息（转换为系统提示） */
    processRecalledMessages(messages: CircleMessage[]): void {
      let processedCount = 0
      
      messages.forEach((msg, index) => {
        if (msg.isRecalled === true && !msg._isSystemTip) {
          // 创建新对象并替换，确保触发响应式更新
          const newMsg: CircleMessage = {
            ...msg,
            content: '',
            _isSystemTip: true,
            _tipType: 'recall' as const,
            _tipUsername: msg.sender?.nickname || ''
          }
          
          // 使用 splice 替换元素，确保触发响应式
          messages.splice(index, 1, newMsg)
          processedCount++
        }
      })
    },

    /** 处理加载的消息中的删除消息（转换为系统提示） */
    processDeletedMessages(messages: CircleMessage[]): void {
      let processedCount = 0
      
      messages.forEach((msg, index) => {
        if (msg.deletedByAdmin === true && !msg._isSystemTip) {
          const senderNickname = msg.sender?.nickname || '未知用户'
          const deleterNickname = msg.deleter?.nickname || '管理员'
          
          // 创建新对象并替换，确保触发响应式更新
          const newMsg: CircleMessage = {
            ...msg,
            content: '',
            _isSystemTip: true,
            _tipType: 'delete' as const,
            _tipUsername: deleterNickname,
            _deleteDetail: `${senderNickname}的消息被${deleterNickname}删除`
          }
          
          // 使用 splice 替换元素，确保触发响应式
          messages.splice(index, 1, newMsg)
          processedCount++
        }
      })
      
      if (processedCount > 0) {

      }
    }
  }
})

export default useCircleChatStore
