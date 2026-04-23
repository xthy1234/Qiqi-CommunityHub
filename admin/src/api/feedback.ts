// src/api/feedback.ts
import httpClient from '@/utils/http'

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

interface PageResponse<T> {
  list: T[]
  total: number
}

interface PageParams {
  page?: number
  limit?: number
  sort?: string
  order?: 'asc' | 'desc'
  [key: string]: any
}

/**
 * 反馈管理 API
 */
export const feedbackApi = {
  /**
   * 获取反馈列表
   */
  getFeedbackList: (params: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>('/feedbacks', { params })
  },

  /**
   * 获取反馈详情
   */
  getFeedbackById: (id: number) => {
    return httpClient.get<ApiResponse<any>>(`/feedbacks/${id}`)
  },

  /**
   * 创建反馈
   */
  createFeedback: (data: any) => {
    return httpClient.post<ApiResponse<any>>('/feedbacks', data)
  },

  /**
   * 回复反馈
   */
  replyFeedback: (id: number, data: { reply: string }) => {
    return httpClient.post<ApiResponse<any>>(`/feedbacks/${id}/reply`, data)
  },

  /**
   * 更新反馈
   */
  updateFeedback: (id: number, data: any) => {
    return httpClient.put<ApiResponse<any>>(`/feedbacks/${id}`, data)
  },

  /**
   * 删除反馈
   */
  deleteFeedback: (id: number) => {
    return httpClient.delete<ApiResponse<any>>(`/feedbacks/${id}`)
  },

  /**
   * 批量删除反馈
   */
  batchDeleteFeedbacks: (ids: number[]) => {
    return httpClient.post<ApiResponse<any>>('/feedbacks/batch-delete', ids)
  }
}

export default feedbackApi
