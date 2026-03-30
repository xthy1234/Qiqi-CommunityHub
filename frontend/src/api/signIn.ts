// src/api/signIn.ts
import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'

/**
 * 签到响应接口
 */
export interface SignInResponse {
  pointsEarned: number        // 今日获得积分
  streak: number              // 连续签到天数
  balance: number             // 当前积分余额
  message: string             // 提示信息
}

/**
 * 积分信息接口
 */
export interface PointsInfo {
  points: number              // 当前积分
  streak: number              // 连续签到天数
  signedIn: boolean           // 今日是否已签到（注意：字段名改为 signedIn）
  lastSignInDate?: string     // 上次签到日期
}

/**
 * 积分流水项接口
 */
export interface PointsTransaction {
  id: number
  userId: number
  amount: number              // 变动金额（正增负减）
  balance: number             // 变动后余额
  source: string              // 来源：sign_in, post_article, like_received 等
  sourceId?: number           // 关联业务 ID
  description: string         // 描述
  createdAt: string           // 创建时间
}

/**
 * 分页参数接口
 */
export interface PaginationParams {
  page: number
  limit: number
  source?: string             // 按来源筛选（可选）
  startDate?: string          // 开始日期（可选）
  endDate?: string            // 结束日期（可选）
}

/**
 * 分页响应接口
 */
export interface TransactionListResponse {
  list: PointsTransaction[]
  total: number
  currPage: number
  pageSize: number
  totalPage: number
}

export interface SignInStatus {
  signedIn: boolean           // 今日是否已签到
  streak: number              // 连续签到天数
}

class SignInService {
  private baseUrl = '/sign-in'

  /**
   * 每日签到
   */
  async doSignIn(): Promise<SignInResponse> {
    const response: AxiosResponse<ApiResponse<SignInResponse>> = await httpClient.post(this.baseUrl)
    return response.data.data
  }

  /**
   * 获取当前用户积分信息
   */
  async getPointsInfo(): Promise<PointsInfo> {
    const response: AxiosResponse<ApiResponse<PointsInfo>> = await httpClient.get(`${this.baseUrl}/points`)
    return response.data.data
  }

  /**
   * 检查今日签到状态
   */
  async getSignInStatus(): Promise<SignInStatus> {
    const response: AxiosResponse<ApiResponse<SignInStatus>> = await httpClient.get(`${this.baseUrl}/status`)
    return response.data.data
  }

  /**
   * 获取积分流水列表
   * @param params 查询参数
   */
  async getTransactions(params: PaginationParams): Promise<TransactionListResponse> {
    const response: AxiosResponse<ApiResponse<TransactionListResponse>> = await httpClient.get(`${this.baseUrl}/transactions`, { params })
    return response.data.data
  }

  /**
   * 获取用户积分总额（供外部调用）
   * @param userId 用户 ID
   */
  async getUserPoints(userId: number): Promise<{ points: number }> {
    const response: AxiosResponse<ApiResponse<{ points: number }>> = await httpClient.get(`/users/${userId}/points`)
    return response.data.data
  }
}

export default new SignInService()
