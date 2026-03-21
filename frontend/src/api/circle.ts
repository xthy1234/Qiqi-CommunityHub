// src/api/circle.ts

import type { Circle, CircleConversation, CircleMessage, CircleMember, PaginationParams, PaginationResult, InviteLinkResponse } from '@/types/circleChat'
import { getWebSocket } from '@/utils/websocket'
import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'

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
    
    const response: AxiosResponse<ApiResponse<PaginationResult<CircleMember>>> = await httpClient.get(`/circles/${circleId}/members`, { params: queryParams })
    return response.data.data
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
  async sendMessage(circleId: number, content: string, msgType: number = 0): Promise<CircleMessage> {
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
    console.log('🔍 [圈子 WebSocket] 开始订阅圈子消息，circleId:', circleId)
    
    // 1. 获取 WebSocket 实例
    const ws = getWebSocket()
    console.log('📡 [圈子 WebSocket] WebSocket 实例:', ws)
    console.log('📡 [圈子 WebSocket] WebSocket 连接状态:', ws?.isConnected ? '已连接' : '未连接')
    
    if (!ws) {
      console.warn('⚠️ [圈子 WebSocket] WebSocket 实例不存在')
      console.log('💡 [圈子 WebSocket] 请检查是否在 main.ts 中调用了 initWebSocket()')
      return () => {}
    }

    // 2. 检查 isConnected 方法是否存在
    console.log('📡 [圈子 WebSocket] isConnected 方法:', typeof ws.isConnected)
    const isWsConnected = ws.isConnected()
    console.log('📡 [圈子 WebSocket] WebSocket 连接结果:', isWsConnected)
    
    if (!isWsConnected) {
      console.warn('⚠️ [圈子 WebSocket] WebSocket 未连接')
      console.log('💡 [圈子 WebSocket] 当前连接状态:')
      console.log('   - client:', (ws as any).client)
      console.log('   - client.connected:', (ws as any).client?.connected)
      return () => {}
    }

    // 3. 获取内部 STOMP client（需要访问私有属性）
    const client = (ws as any).client
    console.log('📡 [圈子 WebSocket] STOMP Client:', client)
    console.log('📡 [圈子 WebSocket] STOMP 连接状态:', client?.connected ? '已连接' : '未连接')
    
    if (!client || !client.connected) {
      console.warn('⚠️ [圈子 WebSocket] STOMP 客户端未连接')
      console.log('💡 [圈子 WebSocket] STOMP 连接失败，请检查:')
      console.log('   1. WebSocket 连接是否正常')
      console.log('   2. STOMP 协议握手是否成功')
      return () => {}
    }

    // 4. 订阅特定圈子的消息
    const destination = `/topic/circles/${circleId}/messages`
    console.log('📬 [圈子 WebSocket] 准备订阅目的地:', destination)
    
    try {
      const subscription = client.subscribe(destination, (message: any) => {
        console.log('✅ [圈子 WebSocket] 收到消息推送:', message)
        console.log('✅ [圈子 WebSocket] 消息内容:', message.body)
        
        try {
          const chatMessage = JSON.parse(message.body) as CircleMessage
          console.log('✅ [圈子 WebSocket] 解析后的消息对象:', chatMessage)
          callback(chatMessage)
        } catch (error) {
          console.error('❌ [圈子 WebSocket] 解析圈子消息失败:', error)
          console.error('❌ [圈子 WebSocket] 原始消息体:', message.body)
        }
      }, {})
      
      console.log('✅ [圈子 WebSocket] 订阅成功，subscription:', subscription)
      
      // 5. 返回取消订阅函数
      return () => {
        console.log('🔕 [圈子 WebSocket] 取消订阅圈子:', circleId)
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
  sendCircleMessage(circleId: number, content: string, msgType: number = 0): void {
    console.log('📤 [圈子 WebSocket] 准备发送消息:', { circleId, content, msgType })
    
    const ws = getWebSocket()
    console.log('📡 [圈子 WebSocket] WebSocket 实例:', ws)
    console.log('📡 [圈子 WebSocket] 连接状态:', ws?.isConnected ? '已连接' : '未连接')
    
    if (!ws || !ws.isConnected()) {
      console.error('❌ [圈子 WebSocket] WebSocket 未连接，无法发送消息')
      console.log('💡 [圈子 WebSocket] 请确保在进入聊天页面之前 WebSocket 已连接')
      return
    }

    const message = {
      circleId: circleId,
      content: content,
      msgType: msgType
    }

    // 使用 STOMP publish 方法
    const client = (ws as any).client
    console.log('📡 [圈子 WebSocket] STOMP Client:', client)
    console.log('📡 [圈子 WebSocket] STOMP 状态:', client?.connected ? '已连接' : '未连接')
    
    if (client && client.connected) {
      try {
        client.publish({
          destination: '/app/circle-message',
          body: JSON.stringify(message)
        })
        console.log('✅ [圈子 WebSocket] 消息发送成功')
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
    console.log('↩️ [圈子 WebSocket] 准备撤回消息:', { messageId, reason })
    
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
        console.log('✅ [圈子 WebSocket] 撤回消息发送成功')
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
    console.log('🗑️ [圈子 WebSocket] 准备删除消息:', messageId)
    
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
        console.log('✅ [圈子 WebSocket] 删除消息发送成功')
      } catch (error) {
        console.error('❌ [圈子 WebSocket] 删除消息失败:', error)
      }
    } else {
      console.error('❌ [圈子 WebSocket] STOMP 客户端未连接')
    }
  }
}
