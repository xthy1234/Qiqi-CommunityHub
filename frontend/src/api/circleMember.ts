// src/api/circleMember.ts
// 圈子成员管理相关接口

import type { CircleMember, PaginationParams, PaginationResult } from '@/types/circleChat'
import type { InviteLinkResponse } from '@/types/circleChat'
import httpClient from '@/utils/http'
import type { ApiResponse } from '@/utils/http'
import type { AxiosResponse } from 'axios'

export const circleMemberApi = {
  /**
   * 获取活跃成员列表（状态为正常的成员）
   */
  async getActiveMembers(circleId: number, params: PaginationParams & { role?: number }): Promise<PaginationResult<CircleMember>> {
    const queryParams: any = {
      page: params.page,
      limit: params.limit
    }
    if (params.role !== undefined) {
      queryParams.role = params.role
    }

    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get(`/circles/${circleId}/active-members`, { params: queryParams })

    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        id: item.id,
        userId: item.user.id,
        circleId: item.circleId,
        nickname: item.user.nickname,
        avatar: item.user.avatar,
        role: item.role,
        roleDescription: item.roleDescription,
        joinTime: item.joinTime,
        status: item.status,
        lastOnlineTime: item.user.lastOnlineTime,
        isOnline: false
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  },

  /**
   * 获取成员列表（兼容旧接口，内部调用 getActiveMembers）
   */
  async getMembers(circleId: number, params: PaginationParams & { role?: number }): Promise<PaginationResult<CircleMember>> {
    return this.getActiveMembers(circleId, params)
  },

  /**
   * 加入公开圈子
   */
  async joinCircle(circleId: number, inviteCode?: string): Promise<void> {
    const data: any = {}
    if (inviteCode) {
      data.inviteCode = inviteCode
    }
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`/circles/${circleId}/members`, data)
    return response.data.data
  },

  /**
   * 申请加入需要审核的圈子
   */
  async applyToJoin(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`/circles/${circleId}/apply`)
    return response.data.data
  },

  /**
   * 接受邀请加入圈子
   */
  async acceptInvite(circleId: number, inviteCode: string): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`/circles/${circleId}/accept-invite`, {
      inviteCode
    })
    return response.data.data
  },

  /**
   * 退出圈子（软删除）
   */
  async leaveCircle(circleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`/circles/${circleId}/members/self`)
    return response.data.data
  },

  /**
   * 移除成员（管理员操作，软删除）
   */
  async removeMember(circleId: number, userId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`/circles/${circleId}/members/${userId}`)
    return response.data.data
  },

  /**
   * 更新成员角色
   */
  async updateMemberRole(circleId: number, userId: number, role: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`/circles/${circleId}/members/${userId}/role`, { role })
    return response.data.data
  },

  /**
   * 邀请成员
   */
  async inviteMember(circleId: number, userId: number): Promise<InviteLinkResponse> {
    const response: AxiosResponse<ApiResponse<InviteLinkResponse>> = await httpClient.post(`/circles/${circleId}/invite`, { userId })
    return response.data.data
  },

  /**
   * 获取待审核申请列表（仅圈主和管理员）
   */
  async getPendingApplications(circleId: number, params: PaginationParams): Promise<PaginationResult<CircleMember>> {
    const response: AxiosResponse<ApiResponse<any>> = await httpClient.get(`/circles/${circleId}/applications`, {
      params: { page: params.page, limit: params.limit }
    })

    const backendData = response.data.data
    return {
      list: backendData.list.map((item: any) => ({
        id: item.id,
        userId: item.user.id,
        circleId: item.circleId,
        nickname: item.user.nickname,
        avatar: item.user.avatar,
        role: item.role,
        roleDescription: item.roleDescription,
        joinTime: item.joinTime,
        status: item.status,
        isOnline: false
      })),
      total: backendData.totalCount,
      page: backendData.currPage,
      limit: backendData.pageSize
    }
  },

  /**
   * 审核加入申请（仅圈主和管理员）
   */
  async reviewApplication(circleId: number, userId: number, approved: boolean, remark?: string): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.put(`/circles/${circleId}/applications/${userId}`, {
      approved,
      remark
    })
    return response.data.data
  }
}

export default circleMemberApi
