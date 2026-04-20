// src/api/circle.ts
// 圈子管理相关接口

import type { Circle, PaginationParams, PaginationResult } from '@/types/circleChat'
import httpClient from '@/utils/http'
import type { ApiResponse } from '@/utils/http'
import type { AxiosResponse } from 'axios'

export const circleApi = {
  /**
   * 获取用户加入的圈子列表
   */
  async getMyCircles(params: PaginationParams): Promise<PaginationResult<Circle>> {
    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get('/circles/mine', {
      params: { page: params.page, limit: params.limit }
    })

    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        id: item.id,
        name: item.name,
        description: item.description,
        avatar: item.avatar,
        ownerId: item.ownerId,
        ownerNickname: item.ownerNickname,
        ownerAvatar: item.ownerAvatar,
        type: item.type,
        memberCount: item.memberCount,
        isJoined: item.isJoined,
        unreadCount: item.unreadCount || 0,
        createTime: item.createTime,
        status: item.status
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  },

  /**
   * 获取圈子详情
   */
  async getCircleById(circleId: number): Promise<Circle> {
    const response: AxiosResponse<ApiResponse<Circle>> = await httpClient.get(`/circles/${circleId}`)
    return response.data.data
  },

  /**
   * 创建圈子
   */
  async createCircle(data: { name: string; description?: string; avatar?: string; type: number }): Promise<Circle> {
    const response: AxiosResponse<ApiResponse<Circle>> = await httpClient.post('/circles', data)
    return response.data.data
  },

  /**
   * 更新圈子信息
   */
  async updateCircle(circleId: number, data: { name?: string; description?: string; avatar?: string; type?: number }): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`/circles/${circleId}`, data)
    return response.data.data
  },

  /**
   * 解散圈子
   */
  async deleteCircle(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`/circles/${circleId}`)
    return response.data.data
  },

  /**
   * 发现公开圈子（支持搜索）
   */
  async getPublicCircles(params: {
    page?: number
    limit?: number
    keyword?: string
  }): Promise<PaginationResult<Circle>> {
    const queryParams: any = {
      page: params.page || 1,
      limit: params.limit || 20
    }
    if (params.keyword) {
      queryParams.keyword = params.keyword
    }

    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get('/circles/public', { params: queryParams })

    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        id: item.id,
        name: item.name,
        description: item.description,
        avatar: item.avatar,
        ownerId: item.ownerId,
        ownerNickname: item.ownerNickname,
        ownerAvatar: item.ownerAvatar,
        type: item.type,
        memberCount: item.memberCount,
        isJoined: item.isJoined,
        unreadCount: item.unreadCount || 0,
        createTime: item.createTime,
        status: item.status
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  },

  /**
   * 获取推荐圈子列表（根据用户兴趣推荐）
   */
  async getRecommendedCircles(params: {
    page?: number
    limit?: number
    keyword?: string
  }): Promise<PaginationResult<Circle>> {
    const queryParams: any = {
      page: params.page || 1,
      limit: params.limit || 20
    }
    if (params.keyword) {
      queryParams.keyword = params.keyword
    }

    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get('/circles/public', { params: queryParams })

    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        id: item.id,
        name: item.name,
        description: item.description,
        avatar: item.avatar,
        ownerId: item.ownerId,
        ownerNickname: item.ownerNickname,
        ownerAvatar: item.ownerAvatar,
        type: item.type,
        memberCount: item.memberCount,
        isJoined: item.isJoined,
        unreadCount: item.unreadCount || 0,
        createTime: item.createTime,
        status: item.status
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  }
}

export default circleApi
