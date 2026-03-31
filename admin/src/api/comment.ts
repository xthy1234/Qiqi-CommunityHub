// src/api/comment.ts
import httpClient from '@/utils/http'

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

interface PageResponse<T> {
  list: T[]
  totalCount: number
  pageSize: number
  totalPage: number
  currPage: number
}

interface PageParams {
  page?: number
  limit?: number
  sort?: string
  order?: 'asc' | 'desc'
  [key: string]: any
}

/**
 * 评论管理 API
 */
export const commentApi = {
  /**
   * 获取评论分页列表（管理员专用）
   * 支持查看所有状态（包括隐藏、已删除）
   */
  getCommentList: (params: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>('/comments/admin/list', { params })
  },

  /**
   * 批量启用/禁用评论（管理员专用）
   * @param status 0-启用，1-禁用
   * @param ids 评论 ID 数组
   */
  batchUpdateStatus: (data: { status: number, ids: number[] }) => {
    return httpClient.post<ApiResponse<any>>(`/comments/admin/batch-update-status?status=${data.status}`, data.ids)
  },

  /**
   * 删除单条评论
   */
  deleteComment: (id: number) => {
    return httpClient.delete<ApiResponse<any>>(`/comments/${id}`)
  },

  /**
   * 批量删除评论
   */
  batchDeleteComments: (ids: number[]) => {
    return httpClient.post<ApiResponse<any>>('/comments/batch-delete', ids)
  },

  /**
   * 启用/禁用评论
   * @param id 评论 ID
   * @param status 0-启用，1-禁用
   */
  updateCommentStatus: (id: number, status: number) => {
    return httpClient.patch<ApiResponse<any>>(`/comments/${id}/status`, { status })
  },

  /**
   * 获取评论详情
   */
  getCommentById: (id: number) => {
    return httpClient.get<ApiResponse<any>>(`/comments/${id}`)
  },

  /**
   * 根据内容 ID 获取评论列表
   */
  getCommentsByContentId: (contentId: number) => {
    return httpClient.get<ApiResponse<any>>(`/comments/content/${contentId}`)
  },

  /**
   * 分页获取内容评论
   */
  getContentCommentsPage: (contentId: number, params: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>(`/comments/content/${contentId}/page`, { params })
  },

  /**
   * 获取评论树结构
   */
  getCommentTree: (contentId: number) => {
    return httpClient.get<ApiResponse<any>>(`/comments/tree/${contentId}`)
  },

  /**
   * 统计评论数量
   */
  getCommentCount: (contentId: number) => {
    return httpClient.get<ApiResponse<number>>(`/comments/count/${contentId}`)
  }
}

export default commentApi
