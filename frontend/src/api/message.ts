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
   * 获取会话列表
   */
  async getConversations(): Promise<ConversationVO[]> {
    const response: AxiosResponse<ApiResponse<ConversationVO[]>> = await httpClient.get(`${this.baseUrl}/conversations`)

    // 确保返回的是数组
    if (response.data && response.data.data) {
      return Array.isArray(response.data.data) ? response.data.data : []
    }
    return []
    // return response.data.data
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
