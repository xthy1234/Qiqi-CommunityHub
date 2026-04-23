// src/api/user.ts
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
 * 用户管理 API
 */
export const userApi = {
  /**
   * 管理员登录
   * @param data 登录信息 {account: 账号，password: 密码，captcha: 验证码}
   */
  adminLogin: (data: { account: string; password: string; captcha?: string }) => {
    return httpClient.post<ApiResponse<{ token: string; user: any }>>('/users/admin/login', data)
  },

  /**
   * 管理员注册
   */
  adminRegister: (data: any) => {
    return httpClient.post<ApiResponse<any>>('/users/admin/register', data)
  },

  /**
   * 用户登录
   */
  login: (data: { username: string; password: string }) => {
    return httpClient.post<ApiResponse<{ token: string; user: any }>>('/users/login', data)
  },

  /**
   * 用户注册
   */
  register: (data: any) => {
    return httpClient.post<ApiResponse<any>>('/users/register', data)
  },

  /**
   * 退出登录
   */
  logout: () => {
    return httpClient.post<ApiResponse>('/users/logout')
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser: () => {
    return httpClient.get<ApiResponse<any>>('/users/me')
  },

  /**
   * 获取用户列表
   */
  getUserList: (params: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>('/users', { params })
  },

  /**
   * 获取用户详情
   */
  getUserById: (id: number) => {
    return httpClient.get<ApiResponse<any>>(`/users/${id}`)
  },

  /**
   * 创建用户
   */
  createUser: (data: any) => {
    return httpClient.post<ApiResponse<any>>('/users', data)
  },

  /**
   * 更新用户
   */
  updateUser: (id: number, data: any) => {
    return httpClient.put<ApiResponse<any>>(`/users/${id}`, data)
  },

  /**
   * 删除用户
   */
  deleteUser: (id: number) => {
    return httpClient.delete<ApiResponse<any>>(`/users/${id}`)
  },

  /**
   * 批量删除用户
   */
  batchDeleteUsers: (ids: number[]) => {
    return httpClient.post<ApiResponse<any>>('/users/batch-delete', ids)
  },

  /**
   * 更新用户密码
   */
  updatePassword: (id: number, data: { oldPassword: string; newPassword: string }) => {
    return httpClient.put<ApiResponse<any>>(`/users/${id}/password`, data)
  },

  /**
   * 重置密码
   */
  resetPassword: (data: { username: string; newPassword: string }) => {
    return httpClient.post<ApiResponse<any>>('/users/reset-password', data)
  }
}

export default userApi
