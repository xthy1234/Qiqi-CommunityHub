// src/api/role.ts
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
 * 分页请求参数
 */
interface PageParams {
  page?: number
  limit?: number
  sort?: string
  order?: 'asc' | 'desc'
  [key: string]: any
}

/**
 * 分页响应数据
 */
interface PageResponse<T> {
  list: T[]
  totalCount: number
  pageSize: number
  totalPage: number
  currPage: number
}

/**
 * Role 实体
 */
export interface Role {
  id?: number
  roleName: string
  hasBackLogin: boolean
  hasBackRegister: boolean
  hasFrontLogin: boolean
  hasFrontRegister: boolean
}

/**
 * 角色管理 API
 */
export const roleApi = {
  /**
   * 获取角色分页列表
   */
  getRoleList: (params: PageParams & { role?: Partial<Role> }) => {
    return httpClient.get<ApiResponse<PageResponse<Role>>>('/roles', { params })
  },

  /**
   * 获取所有角色列表（无分页）
   */
  getAllRoles: (role?: Partial<Role>) => {
    return httpClient.get<ApiResponse<Role[]>>('/roles/all', { params: role })
  },

  /**
   * 获取角色详情
   */
  getRoleById: (id: number) => {
    return httpClient.get<ApiResponse<Role>>(`/roles/${id}`)
  },

  /**
   * 统计角色数量
   */
  countRoles: (roleName?: string) => {
    return httpClient.get<ApiResponse<number>>('/roles/count', {
      params: { roleName }
    })
  },

  /**
   * 创建角色
   */
  createRole: (data: Role) => {
    return httpClient.post<ApiResponse<Role>>('/roles', data)
  },

  /**
   * 更新角色信息
   */
  updateRole: (id: number, data: Role) => {
    return httpClient.put<ApiResponse<Role>>(`/roles/${id}`, data)
  },

  /**
   * 部分更新角色
   */
  patchRole: (id: number, data: Partial<Role>) => {
    return httpClient.patch<ApiResponse<Role>>(`/roles/${id}`, data)
  },

  /**
   * 删除单个角色
   */
  deleteRole: (id: number) => {
    return httpClient.delete<ApiResponse<void>>(`/roles/${id}`)
  },

  /**
   * 批量删除角色
   */
  batchDeleteRoles: (ids: number[]) => {
    return httpClient.delete<ApiResponse<void>>('/roles', { data: ids })
  }
}

export default roleApi

