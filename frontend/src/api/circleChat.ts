// src/api/circleChat.ts
// 圈子聊天相关接口

import type { CircleConversation, CircleMessage, PaginationParams, PaginationResult } from '@/types/circleChat'
import httpClient from '@/utils/http'
import type { ApiResponse } from '@/utils/http'
import type { AxiosResponse } from 'axios'

export const circleChatApi = {
  /**
   * 获取会话列表
   */
  async getConversations(params: PaginationParams): Promise<PaginationResult<CircleConversation>> {
    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get('/circles/chat/conversations', {
      params: { page: params.page, limit: params.limit }
    })

    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        circleId: item.circleId,
        circleName: item.circleName,
        circleAvatar: item.circleAvatar,
        lastMessageId: item.lastMessageId,
        lastMessageContent: item.lastMessageContent,
        lastMessageSenderId: item.lastMessageSenderId,
        lastMessageSenderNickname: item.lastMessageSenderNickname,
        lastMessageTime: item.lastMessageTime,
        unreadCount: item.unreadCount,
        memberCount: item.memberCount
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  },

  /**
   * 获取聊天记录
   */
  async getChatHistory(circleId: number, params: PaginationParams): Promise<PaginationResult<CircleMessage>> {
    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get(`/circles/${circleId}/chat/history`, {
      params: { page: params.page, limit: params.limit }
    })

    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        id: item.id,
        circleId: item.circleId,
        senderId: item.senderId,
        sender: item.sender,
        content: item.content,
        msgType: item.msgType,
        isRecalled: item.isRecalled,
        recallReason: item.recallReason,
        createTime: item.createTime,
        isSelf: item.isSelf
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  },

  /**
   * 统计未读消息数
   */
  async getUnreadCount(circleId: number): Promise<number> {
    const response: AxiosResponse<ApiResponse<number>> = await httpClient.get(`/circles/${circleId}/chat/unread-count`)
    return response.data.data
  },

  /**
   * 发送消息（HTTP 方式，推荐使用 WebSocket）
   */
  async sendMessage(circleId: number, data: { content: any; msgType: string }): Promise<CircleMessage> {
    const response: AxiosResponse<ApiResponse<CircleMessage>> = await httpClient.post(`/circles/${circleId}/chat/messages`, data)
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
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(
        `/circles/${circleId}/chat/messages/${messageId}/recall`,
        null,
        { params }
    )
    return response.data.data
  },

  /**
   * 获取最新消息
   */
  async getLatestMessage(circleId: number): Promise<CircleMessage> {
    const response: AxiosResponse<ApiResponse<CircleMessage>> = await httpClient.get(`/circles/${circleId}/chat/latest`)
    return response.data.data
  },

  /**
   * 标记消息为已读
   */
  async markAsRead(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`/circles/${circleId}/chat/read`)
    return response.data.data
  }
}

export default circleChatApi
