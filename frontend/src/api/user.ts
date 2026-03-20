// src/api/user.ts
import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'

/**
 * 用户信息接口
 */
export interface User {
  id: number
  account: string
  nickname?: string
  avatar?: string
  gender?: number
  phone?: string
  email?: string
  roleId?: number
  birthday?: string
  signature?: string
  status?: number
  isDeleted?: number
  createTime?: string
  updateTime?: string
}

/**
 * 登录参数接口
 */
export interface LoginParams {
  account: string
  password: string
}

/**
 * 注册参数接口
 */
export interface RegisterParams extends Partial<User> {
  password: string
  confirmPassword?: string
}

/**
 * 分页列表返回类型
 */
export interface UserListResponse {
  list: User[]
  total: number
  currPage: number
  pageSize: number
  totalPage: number
}

class UserService {
  private baseUrl = '/users'

  /**
   * 用户登录
   * @param params 登录参数
   */
  async login(params: LoginParams): Promise<{ token: string }> {
    const response: AxiosResponse<ApiResponse<{ token: string }>> = await httpClient.post(`${this.baseUrl}/login`, params)
   return response.data.data
  }

  /**
   * 管理员登录
   * @param params 登录参数
   */
  async adminLogin(params: LoginParams): Promise<{ token: string }> {
    const response: AxiosResponse<ApiResponse<{ token: string }>> = await httpClient.post(`${this.baseUrl}/admin/login`, params)
   return response.data.data
  }

  /**
   * 退出登录
   */
  async logout(): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`${this.baseUrl}/logout`)
    return response.data.data
  }

  /**
   * 获取当前用户信息
   */
  async getCurrentUser(): Promise<User> {
    const response: AxiosResponse<ApiResponse<User>> = await httpClient.get(`${this.baseUrl}/me`)
    return response.data.data
  }

  /**
   * 获取用户详情
   * @param id 用户 ID
   */
  async getUserById(id: number): Promise<User> {
    const response: AxiosResponse<ApiResponse<User>> = await httpClient.get(`${this.baseUrl}/${id}`)
    return response.data.data
  }

  /**
   * 创建用户 (注册)
   * @param data 用户数据
   */
  async createUser(data: RegisterParams): Promise<User> {
    const response: AxiosResponse<ApiResponse<User>> = await httpClient.post(`${this.baseUrl}/register`, data)
   return response.data.data
  }

  /**
   * 更新用户信息
   * @param id 用户 ID
   * @param data 用户数据
   */
  async updateUser(id: number, data: Partial<User>): Promise<User> {
    const response: AxiosResponse<ApiResponse<User>> = await httpClient.put(`${this.baseUrl}/${id}`, data)
    return response.data.data
  }

  /**
   * 部分更新用户
   * @param id 用户 ID
   * @param data 用户数据
   */
  async partialUpdateUser(id: number, data: Partial<User>): Promise<User> {
    const response: AxiosResponse<ApiResponse<User>> = await httpClient.patch(`${this.baseUrl}/${id}`, data)
    return response.data.data
  }

  /**
   * 更新用户密码
   * @param id 用户 ID
   * @param oldPassword 原密码
   * @param newPassword 新密码
   */
  async updatePassword(id: number, oldPassword: string, newPassword: string): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`${this.baseUrl}/${id}/password`, null, { 
      params: { oldPassword, newPassword } 
    })
    return response.data.data
  }

  /**
   * 更新用户头像
   * @param id 用户 ID
   * @param avatar 头像 URL
   */
  async updateAvatar(id: number, avatar: string): Promise<User> {
    const response: AxiosResponse<ApiResponse<User>> = await httpClient.patch(`${this.baseUrl}/${id}/avatar`, { avatar })
    return response.data.data
  }

  /**
   * 重置密码
   * @param account 账号
   * @param newPassword 新密码
   */
  async resetPassword(account: string, newPassword: string): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`${this.baseUrl}/reset-password`, null, { 
      params: { account, newPassword } 
    })
    return response.data.data
  }

  /**
   * 启用/禁用用户
   * @param id 用户 ID
   * @param status 状态
   */
  async updateStatus(id: number, status: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.patch(`${this.baseUrl}/${id}/status`, { status })
    return response.data.data
  }

  /**
   * 删除用户
   * @param id 用户 ID
   */
  async deleteUser(id: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`${this.baseUrl}/${id}`)
    return response.data.data
  }

  /**
   * 批量删除用户
   * @param ids 用户 ID 数组
   */
  async batchDeleteUsers(ids: number[]): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`${this.baseUrl}/batch-delete`, { ids })
    return response.data.data
  }

  /**
   * 统计用户总数
   */
  async count(): Promise<number> {
    const response: AxiosResponse<ApiResponse<number>> = await httpClient.get(`${this.baseUrl}/count`)
    return response.data.data
  }

  /**
   * 获取用户分页列表
   * @param params 查询参数
   */
  async getUserList(params: { page: number; limit: number; [key: string]: any }): Promise<{ list: User[]; total: number }> {
    const response: AxiosResponse<ApiResponse<{ list: User[]; total: number }>> = await httpClient.get(`${this.baseUrl}`, { params })
    return response.data.data
  }

  /**
   * 获取推荐用户列表
   * @param params 查询参数
   */
  async getRecommendedUsers(params: { 
    page?: number
    limit?: number
    keyword?: string 
  }): Promise<UserListResponse> {
    const queryParams: any = {
      page: params.page || 1,
      limit: params.limit || 20
    }
    if (params.keyword) {
      queryParams.keyword = params.keyword
    }
    
    const response: AxiosResponse<ApiResponse<UserListResponse>> = await httpClient.get(`${this.baseUrl}/public/list`, { params: queryParams })
    return response.data.data
  }
}

export default new UserService()
