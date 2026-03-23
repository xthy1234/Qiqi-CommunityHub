// src/api/follow.ts
import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'

/**
 * 关注记录接口
 */
export interface FollowRecord {
  id: number
  userId: number
  username: string
  avatar?: string
  signature?: string
  gender?: number
  followTime: string
}

/**
 * 分页数据接口
 */
export interface PaginationData<T> {
  total: number
  list: T[]
  page?: number
  limit?: number
}

/**
 * 关注操作参数
 */
export interface FollowActionParams {
  userId: number
  action: 'follow' | 'unfollow'
}

/**
 * 关注状态查询结果
 */
export interface FollowStatusMap {
  [userId: number]: boolean
}

/**
 * Follow API 服务类
 */
class FollowService {
  private baseUrl = '/follow'

  /**
   * 关注/取关用户
   * @param params 操作参数
   */
  async followOrUnfollow(params: FollowActionParams): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(this.baseUrl, params)
    return response.data.data
  }

  /**
   * 获取用户的关注列表
   * @param userId 用户ID
   * @param page 页码
   * @param limit 每页数量
   */
  async getFollowingList(
    userId: number, 
    page = 1, 
    limit = 20
  ): Promise<PaginationData<FollowRecord>> {
    const response: AxiosResponse<ApiResponse<PaginationData<FollowRecord>>> = await httpClient.get(
      `${this.baseUrl}/following`,
      { params: { userId, page, limit } }
    )
    return response.data.data
  }

  /**
   * 获取用户的粉丝列表
   * @param userId 用户ID
   * @param page 页码
   * @param limit 每页数量
   */
  async getFollowerList(
    userId: number, 
    page = 1, 
    limit = 20
  ): Promise<PaginationData<FollowRecord>> {
    const response: AxiosResponse<ApiResponse<PaginationData<FollowRecord>>> = await httpClient.get(
      `${this.baseUrl}/followers`,
      { params: { userId, page, limit } }
    )
    return response.data.data
  }

  /**
   * 批量查询关注状态
   * @param targetIds 目标用户ID 数组
   */
  async getFollowStatus(targetIds: number[]): Promise<FollowStatusMap> {
    const response: AxiosResponse<ApiResponse<FollowStatusMap>> = await httpClient.get(
      `${this.baseUrl}/status`,
      { params: { targetIds: targetIds.join(',') } }
    )
    return response.data.data
  }

  /**
   * 查询是否为好友（互关）
   * @param userId 目标用户ID
   */
  async isFriend(userId: number): Promise<boolean> {
    const response: AxiosResponse<ApiResponse<boolean>> = await httpClient.get(
      `${this.baseUrl}/is-friend/${userId}`
    )
    return response.data.data
  }

  /**
   * 统计关注数
   * @param userId 用户ID
   */
  async countFollowing(userId: number): Promise<number> {
    const response: AxiosResponse<ApiResponse<number>> = await httpClient.get(
      `${this.baseUrl}/count/following`,
      { params: { userId } }
    )
    return response.data.data
  }

  /**
   * 统计粉丝数
   * @param userId 用户ID
   */
  async countFollowers(userId: number): Promise<number> {
    const response: AxiosResponse<ApiResponse<number>> = await httpClient.get(
      `${this.baseUrl}/count/followers`,
      { params: { userId } }
    )
    return response.data.data
  }
}

export default new FollowService()
