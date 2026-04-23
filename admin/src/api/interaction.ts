// src/api/interaction.ts
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
 * 互动管理 API（点赞、收藏等）
 */
export const interactionApi = {
  /**
   * 获取互动列表
   */
  getInteractionList: (params: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>('/interactions', { params })
  },

  /**
   * 获取互动详情
   */
  getInteractionById: (id: number) => {
    return httpClient.get<ApiResponse<any>>(`/interactions/${id}`)
  },

  /**
   * 添加互动
   */
  createInteraction: (data: any) => {
    return httpClient.post<ApiResponse<any>>('/interactions', data)
  },

  /**
   * 点赞/点踩
   */
  like: (data: { type: string; targetId: number; action: 'like' | 'dislike' }) => {
    return httpClient.post<ApiResponse<any>>('/interactions/like', data)
  },

  /**
   * 取消点赞
   */
  unlike: (data: { type: string; targetId: number }) => {
    return httpClient.delete<ApiResponse<any>>('/interactions/like', { data })
  },

  /**
   * 取消互动
   */
  cancelInteraction: (data: any) => {
    return httpClient.delete<ApiResponse<any>>('/interactions/action', { data })
  },

  /**
   * 删除互动
   */
  deleteInteraction: (id: number) => {
    return httpClient.delete<ApiResponse<any>>(`/interactions/${id}`)
  },

  /**
   * 批量删除互动
   */
  batchDeleteInteractions: (ids: number[]) => {
    return httpClient.post<ApiResponse<any>>('/interactions/batch-delete', ids)
  }
}

export default interactionApi
