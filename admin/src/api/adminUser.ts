// src/api/adminUser.ts
import httpClient from '@/utils/http'

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

interface PageParams {
  page?: number
  limit?: number
  sort?: string
  order?: 'asc' | 'desc'
  [key: string]: any
}

interface PageResponse<T> {
  total: number
  size: number
  current: number
  records: T[]
}

/**
 * 管理员专用用户接口
 */
export const adminUserApi = {
  /**
   * 获取用户分页列表（管理员专用）
   * @param params 查询参数
   */
  getUserList: (params: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>('/users', { params })
  },

  /**
   * 获取用户完整信息（管理员专用）
   * @param id 用户 ID
   */
  getAdminUserById: (id: number) => {
    return httpClient.get<ApiResponse<any>>(`/users/admin/${id}`)
  },

  /**
   * 更新用户信息（管理员专用）
   * @param id 用户 ID
   * @param data 用户信息
   */
  updateAdminUser: (id: number, data: any) => {
    return httpClient.put<ApiResponse<any>>(`/users/admin/${id}`, data)
  },

  /**
   * 创建用户
   * @param data 用户信息
   */
  createUser: (data: any) => {
    return httpClient.post<ApiResponse<any>>('/users', data)
  },

  /**
   * 删除用户
   * @param id 用户 ID
   */
  deleteUser: (id: number) => {
    return httpClient.delete<ApiResponse<any>>(`/users/${id}`)
  }
}

export default adminUserApi
