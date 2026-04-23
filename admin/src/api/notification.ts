// src/api/notification.ts
import httpClient from '@/utils/http'

/**
 * API 响应数据结构
 */
interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

/**
 * 分页响应数据
 */
interface PageResponse<T> {
  list: T[]
  total: number
}

/**
 * 发送通知请求参数
 */
export interface SendNotificationRequest {
  userIds?: number[] | null
  title: string
  content: string
  linkUrl?: string | null
  priority?: number
  isTop?: boolean
  extra?: Record<string, any>
}

/**
 * 通知管理 API
 */
export const notificationApi = {
  /**
   * 发送系统通知
   * @param data - 通知数据
   * @returns Promise<ApiResponse<{ sentCount: number }>>
   */
  sendNotification: (data: SendNotificationRequest) => {
    return httpClient.post<ApiResponse<{ sentCount: number }>>('/notifications/send', data)
  },

  /**
   * 获取用户列表（用于通知发送时的用户搜索）
   * @param params - 查询参数
   */
  getUserListForNotification: (params: { keyword?: string; page?: number; limit?: number }) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>('/users', { params })
  }
}

export default notificationApi
