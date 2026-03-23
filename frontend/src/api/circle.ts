// src/api/circle.ts

import type {
  Circle,
  CircleConversation,
  CircleMember,
  CircleMessage,
  InviteLinkResponse,
  PaginationParams,
  PaginationResult
} from '@/types/circleChat'
import {getWebSocket, initWebSocket} from '@/utils/websocket'
import type {ApiResponse} from '@/utils/http'
import httpClient from '@/utils/http'
import type {AxiosResponse} from 'axios'

/**
 * 初始化圈子聊天的 WebSocket连接
 * 确保在使用圈子聊天功能前调用
 */
export function ensureCircleWebSocketConnected(): Promise<void> {
  return new Promise((resolve, reject) => {
    const ws = getWebSocket()
    
    if (!ws) {
      // WebSocket 实例不存在，需要初始化
      console.warn('⚠️ [圈子聊天] WebSocket 实例不存在，正在初始化...')
      
      try {
        const wsManager = initWebSocket()
        wsManager.connect()
          .then(() => {
            console.log('✅ [圈子聊天] WebSocket 初始化成功')
            resolve()
          })
          .catch((error) => {
            console.error('❌ [圈子聊天] WebSocket 初始化失败:', error)
            reject(error)
          })
      } catch (error) {
        console.error('❌ [圈子聊天] WebSocket 初始化异常:', error)
        reject(error)
      }
    } else if (!ws.isConnected()) {
      // WebSocket 实例存在但未连接
      console.warn('⚠️ [圈子聊天] WebSocket 未连接，正在重新连接...')
      
      ws.connect()
        .then(() => {
          console.log('✅ [圈子聊天] WebSocket 重连成功')
          resolve()
        })
        .catch((error) => {
          console.error('❌ [圈子聊天] WebSocket 重连失败:', error)
          reject(error)
        })
    } else {
      // WebSocket 已连接
      resolve()
    }
  })
}

/**
 * 圈子管理相关接口
 */
export const circleApi = {
  /**
   * 获取用户加入的圈子列表
   */
  async getMyCircles(params: PaginationParams): Promise<PaginationResult<Circle>> {
    const response: AxiosResponse<ApiResponse<PaginationResult<Circle>>> = await httpClient.get('/circles/mine', { 
      params: { page: params.page, limit: params.limit }
    })
    return response.data.data
  },

  /**
   * 获取圈子详情
   */
  async getCircleById(circleId: number): Promise<Circle> {
    const response: AxiosResponse<ApiResponse<Circle>> = await httpClient.get(`/circles/${circleId}`)
    return response.data.data
  },

  /**
   * 创建圈子
   */
  async createCircle(data: { name: string; description?: string; avatar?: string; type: number }): Promise<Circle> {
    const response: AxiosResponse<ApiResponse<Circle>> = await httpClient.post('/circles', data)
    return response.data.data
  },

  /**
   * 更新圈子信息
   */
  async updateCircle(circleId: number, data: { name?: string; description?: string; avatar?: string; type?: number }): Promise<Circle> {
    const response: AxiosResponse<ApiResponse<Circle>> = await httpClient.put(`/circles/${circleId}`, data)
    return response.data.data
  },

  /**
   * 解散圈子
   */
  async deleteCircle(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`/circles/${circleId}`)
    return response.data.data
  },

  /**
   * 退出圈子
   */
  async leaveCircle(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`/circles/${circleId}/leave`)
    return response.data.data
  }
}

/**
 * 圈子成员管理接口
 */
export const circleMemberApi = {
  /**
   * 获取成员列表
   */
  async getMembers(circleId: number, params: PaginationParams & { role?: number }): Promise<PaginationResult<CircleMember>> {
    const queryParams: any = {
      page: params.page,
      limit: params.limit
    }
    if (params.role !== undefined) {
      queryParams.role = params.role
    }
    
    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get(`/circles/${circleId}/members`, { params: queryParams })
    
    // 转换后端返回的数据结构到前端期望的格式
    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        id: item.id,
        userId: item.user.id,
        circleId: item.circleId,
        nickname: item.user.nickname,
        avatar: item.user.avatar,
        role: item.role,
        joinTime: item.joinTime,
        isOnline: false // 后端未提供在线状态，暂设为false
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  },

  /**
   * 加入圈子
   */
  async joinCircle(circleId: number, inviteCode?: string): Promise<void> {
    const data: any = {}
    if (inviteCode) {
      data.inviteCode = inviteCode
    }
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`/circles/${circleId}/members`, data)
    return response.data.data
  },

  /**
   * 退出圈子（自行退出）
   */
  async leaveCircleSelf(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`/circles/${circleId}/members/self`)
    return response.data.data
  },

  /**
   * 移除成员
   */
  async removeMember(circleId: number, userId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`/circles/${circleId}/members/${userId}`)
    return response.data.data
  },

  /**
   * 更新成员角色
   */
  async updateMemberRole(circleId: number, userId: number, role: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`/circles/${circleId}/members/${userId}/role`, { role })
    return response.data.data
  },

  /**
   * 邀请成员
   */
  async inviteMember(circleId: number, userId: number): Promise<InviteLinkResponse> {
    const response: AxiosResponse<ApiResponse<InviteLinkResponse>> = await httpClient.post(`/circles/${circleId}/invite`, { userId })
    return response.data.data
  },

  /**
   * 获取推荐圈子列表
   */
  async getRecommendedCircles(params: { 
    page?: number
    limit?: number
    keyword?: string 
  }): Promise<PaginationResult<Circle>> {
    const queryParams: any = {
      page: params.page || 1,
      limit: params.limit || 20
    }
    if (params.keyword) {
      queryParams.keyword = params.keyword
    }
    
    const response: AxiosResponse<ApiResponse<PaginationResult<Circle>>> = await httpClient.get('/circles/public', { params: queryParams })
    return response.data.data
  }
}

/**
 * 圈子聊天接口
 */
export const circleChatApi = {
  /**
   * 获取会话列表
   */
  async getConversations(params: PaginationParams): Promise<PaginationResult<CircleConversation>> {
    const response: AxiosResponse<ApiResponse<PaginationResult<CircleConversation>>> = await httpClient.get('/circles/chat/conversations', { 
      params: { page: params.page, limit: params.limit }
    })
    return response.data.data
  },

  /**
   * 获取未读消息数
   */
  async getUnreadCount(circleId: number): Promise<number> {
    const response: AxiosResponse<ApiResponse<number>> = await httpClient.get(`/circles/${circleId}/chat/unread-count`)
    return response.data.data
  },

  /**
   * 获取聊天记录
   */
  async getChatHistory(circleId: number, params: PaginationParams): Promise<PaginationResult<CircleMessage>> {
    const response: AxiosResponse<ApiResponse<PaginationResult<CircleMessage>>> = await httpClient.get(`/circles/${circleId}/chat/history`, { 
      params: { page: params.page, limit: params.limit }
    })
    return response.data.data
  },

  /**
   * 发送消息（HTTP 方式，不推荐，仅作为降级方案）
   */
  async sendMessage(circleId: number, content: any, msgType = 0): Promise<CircleMessage> {
    const response: AxiosResponse<ApiResponse<CircleMessage>> = await httpClient.post(`/circles/${circleId}/chat/messages`, { 
      content, 
      msgType 
    })
    return response.data.data
  },

  /**
   * 撤回消息
   */
  async recallMessage(circleId: number, messageId: number, reason?: string): Promise<void> {
    const params: any = {}
    if (reason) {
      params.reason = reason
    }
    
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`/circles/${circleId}/chat/messages/${messageId}/recall`, null, { params })
    return response.data.data
  },

  /**
   * 标记圈子消息为已读
   */
  async markAsRead(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`/circles/${circleId}/chat/read`)
    return response.data.data
  },

  /**
   * 删除消息（仅群主和管理员可用）
   */
  async deleteMessage(circleId: number, messageId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`/circles/${circleId}/chat/messages/${messageId}`)
    return response.data.data
  }
}

/**
 * WebSocket 相关操作
 */
export const circleWebSocket = {
  /**
   * 订阅圈子消息主题
   */
  subscribeCircleMessages(circleId: number, callback: (message: CircleMessage) => void): () => void {
    // 1. 获取 WebSocket 实例
    const ws = getWebSocket()
    if (!ws) {
      console.warn('⚠️ [圈子 WebSocket] WebSocket 实例不存在')

      return () => {}
    }

    // 2. 检查 isConnected 方法是否存在

    const isWsConnected = ws.isConnected()
    
    if (!isWsConnected) {
      console.warn('⚠️ [圈子 WebSocket] WebSocket 未连接')

      return () => {}
    }

    // 3. 获取内部 STOMP client（需要访问私有属性）
    const client = (ws as any).client

    if (!client || !client.connected) {
      console.warn('⚠️ [圈子 WebSocket] STOMP 客户端未连接')
      return () => {}
    }

    // 4. 订阅特定圈子的消息
    const destination = `/topic/circles/${circleId}/messages`
    
    try {
      const subscription = client.subscribe(destination, (message: any) => {
        
        try {
          const chatMessage = JSON.parse(message.body) as CircleMessage

          callback(chatMessage)
        } catch (error) {
          console.error('❌ [圈子 WebSocket] 解析圈子消息失败:', error)
          console.error('❌ [圈子 WebSocket] 原始消息体:', message.body)
        }
      }, {})
      
      // 5. 返回取消订阅函数
      return () => {

        subscription?.unsubscribe()
      }
    } catch (error) {
      console.error('❌ [圈子 WebSocket] 订阅失败:', error)
      return () => {}
    }
  },

  /**
   * 发送圈子消息
   */
  sendCircleMessage(circleId: number, chatMessage: any): void {
  
  const ws = getWebSocket()
  
  if (!ws || !ws.isConnected()) {
    console.error('❌ [圈子 WebSocket] WebSocket 未连接，无法发送消息')
    return
  }

  // chatMessage 已经是完整的对象格式：{ circleId, content(对象), msgType, extra }
  // 直接使用传入的完整对象
  const message = {
    circleId: circleId,
    content: chatMessage.content,  // TipTap JSON 对象（不要 stringify）
    msgType: chatMessage.msgType,
    extra: chatMessage.extra || {}
  }

  // 关键新增：详细的日志输出
  
  // 验证 content 是否为完整的 TipTap 文档对象
  if (typeof message.content !== 'object' || message.content === null) {
    console.error('❌ [圈子 API] content 不是对象:', typeof message.content)
    return
  }
  
  if (!message.content.type) {
    console.error('❌ [圈子 API] content 缺少 type 字段，这不是有效的 TipTap JSON')
    console.error('   实际内容:', message.content)
    return
  }
  
  if (message.content.type !== 'doc') {
    console.error('❌ [圈子 API] content.type 不是 "doc"，而是:', message.content.type)
    console.error('   这可能导致后端反序列化失败')
  }

  // 使用 STOMP publish 方法
  const client = (ws as any).client
  
  if (client && client.connected) {
    try {
      // 只对整个对象进行一次 JSON.stringify
      // content 字段会作为嵌套的 JSON 对象一起序列化
      const serializedBody = JSON.stringify(message)
      
      client.publish({
        destination: '/app/circle-message',
        body: serializedBody
      })

    } catch (error) {
      console.error('❌ [圈子 WebSocket] 消息发送失败:', error)
    }
  } else {
    console.error('❌ [圈子 WebSocket] STOMP 客户端未连接')
  }
},

  /**
   * 撤回消息
   */
  recallMessage(messageId: number, reason?: string): void {
    
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      console.error('❌ [圈子 WebSocket] WebSocket 未连接，无法撤回消息')
      return
    }

    const request = {
      messageId: messageId,
      reason: reason || ''
    }

    // 使用 STOMP publish 方法
    const client = (ws as any).client
    if (client && client.connected) {
      try {
        client.publish({
          destination: '/app/circle-recall-message',
          body: JSON.stringify(request)
        })

      } catch (error) {
        console.error('❌ [圈子 WebSocket] 撤回消息失败:', error)
      }
    } else {
      console.error('❌ [圈子 WebSocket] STOMP 客户端未连接')
    }
  },

  /**
   * 删除消息（仅群主和管理员）
   */
  deleteMessage(messageId: number): void {
    
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      console.error('❌ [圈子 WebSocket] WebSocket 未连接，无法删除消息')
      return
    }

    const request = {
      messageId: messageId
    }

    // 使用 STOMP publish 方法
    const client = (ws as any).client
    if (client && client.connected) {
      try {
        client.publish({
          destination: '/app/circle-delete-message',
          body: JSON.stringify(request)
        })

      } catch (error) {
        console.error('❌ [圈子 WebSocket] 删除消息失败:', error)
      }
    } else {
      console.error('❌ [圈子 WebSocket] STOMP 客户端未连接')
    }
  }
}
