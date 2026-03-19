import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'
import type { Message, MessageSendDTO, ConversationVO, MessageSendResponseVO } from '@/types/message'

/**
 * 消息 API 服务类
 */
class MessageService {
  private baseUrl = '/messages'

  /**
   * 发送私信
   * @param params 发送消息参数
   */
  async sendMessage(params: MessageSendDTO): Promise<MessageSendResponseVO> {
    const response: AxiosResponse<ApiResponse<MessageSendResponseVO>> = await httpClient.post(this.baseUrl, params)
    return response.data.data
  }

  /**
   * 获取聊天记录
   * @param userId 对方用户 ID
   * @param page 页码
   * @param limit 每页数量
   */
  async getChatHistory(userId: number, page: number = 1, limit: number = 20): Promise<{ list: Message[]; total: number }> {
    const response: AxiosResponse<ApiResponse<{ list: Message[]; total: number }>> = await httpClient.get(`${this.baseUrl}/with/${userId}`, {
      params: { page, limit }
    })
    return response.data.data
  }

  /**
   * 获取与指定用户的会话信息（用于判断是否已有会话）
   * @param userId 对方用户 ID
   */
  async getConversationWithUser(userId: number): Promise<ConversationVO | null> {
    try {
      const response: AxiosResponse<ApiResponse<ConversationVO>> = await httpClient.get(`${this.baseUrl}/conversation/${userId}`)
      return response.data.data
    } catch (error) {
      // 如果会话不存在，后端可能返回 404，此时返回 null
      return null
    }
  }

  /**
   * 获取会话列表
   */
  async getConversations(): Promise<ConversationVO[]> {
    const response: AxiosResponse<ApiResponse<{ list: ConversationVO[]; totalCount?: number }>> = await httpClient.get(`${this.baseUrl}/conversations`)

    //  处理分页响应格式
    if (response.data && response.data.data) {
      const data = response.data.data
      // 如果是分页对象，返回 list 数组
      if (typeof data === 'object' && 'list' in data) {
        return Array.isArray(data.list) ? data.list : []
      }
      // 如果直接是数组，直接返回
      return Array.isArray(data) ? data : []
    }
    return []
  }

  /**
   * 标记消息已读
   * @param fromUserId 消息发送方用户 ID
   */
  async markAsRead(fromUserId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`${this.baseUrl}/read`, { fromUserId })
    return response.data.data
  }

  /**
   * 删除单条消息
   * @param messageId 消息 ID
   */
  async deleteMessage(messageId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`${this.baseUrl}/${messageId}`)
    return response.data.data
  }

  /**
   * 批量删除消息
   * @param messageIds 消息 ID 数组
   */
  async batchDeleteMessages(messageIds: number[]): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`${this.baseUrl}/batch-delete`, { ids: messageIds })
    return response.data.data
  }
}

export default new MessageService()
