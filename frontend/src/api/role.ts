// src/api/role.ts
import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'

/**
 * 角色接口
 */
export interface Role {
  id: number
  name: string
  code: string
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 角色 API 服务类
 */
class RoleService {
  private baseUrl = '/roles'

  /**
   * 获取角色列表（前端，无分页，无需认证）
   */
  async getAllRoles(): Promise<Role[]> {
    const response: AxiosResponse<ApiResponse<Role[]>> = await httpClient.get(`${this.baseUrl}/all`)
    return response.data.data
  }

  /**
   * 获取角色详情
   * @param id 角色 ID
   */
  async getRoleById(id: number): Promise<Role> {
    const response: AxiosResponse<ApiResponse<Role>> = await httpClient.get(`${this.baseUrl}/${id}`)
    return response.data.data
  }

  /**
   * 创建角色
   * @param data 角色数据
   */
  async createRole(data: Role): Promise<Role> {
    const response: AxiosResponse<ApiResponse<Role>> = await httpClient.post(this.baseUrl, data)
    return response.data.data
  }

  /**
   * 更新角色信息
   * @param id 角色 ID
   * @param data 角色数据
   */
  async updateRole(id: number, data: Role): Promise<Role> {
    const response: AxiosResponse<ApiResponse<Role>> = await httpClient.put(`${this.baseUrl}/${id}`, data)
    return response.data.data
  }

  /**
   * 部分更新角色
   * @param id 角色 ID
   * @param data 角色数据
   */
  async partialUpdateRole(id: number, data: Partial<Role>): Promise<Role> {
    const response: AxiosResponse<ApiResponse<Role>> = await httpClient.patch(`${this.baseUrl}/${id}`, data)
    return response.data.data
  }

  /**
   * 删除角色
   * @param id 角色 ID
   */
  async deleteRole(id: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`${this.baseUrl}/${id}`)
    return response.data.data
  }

  /**
   * 批量删除角色
   * @param ids 角色 ID 数组
   */
  async batchDeleteRoles(ids: number[]): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(this.baseUrl, { data: { ids } })
    return response.data.data
  }

  /**
   * 统计角色数量
   */
  async count(): Promise<number> {
    const response: AxiosResponse<ApiResponse<number>> = await httpClient.get(`${this.baseUrl}/count`)
    return response.data.data
  }

  /**
   * 获取角色分页列表（后台）
   * @param params 查询参数
   */
  async getRoleList(params: { page: number; limit: number; [key: string]: any }): Promise<{ list: Role[]; total: number }> {
    const response: AxiosResponse<ApiResponse<{ list: Role[]; total: number }>> = await httpClient.get(this.baseUrl, { params })
    return response.data.data
  }
}

export default new RoleService()
