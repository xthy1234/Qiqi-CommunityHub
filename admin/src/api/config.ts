// src/api/config.ts
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
 * 配置管理 API
 */
export const configApi = {
  /**
   * 获取配置列表
   */
  getConfigList: (params: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<any>>>('/configs', { params })
  },

  /**
   * 获取配置详情
   */
  getConfigById: (id: number) => {
    return httpClient.get<ApiResponse<any>>(`/configs/${id}`)
  },

  /**
   * 根据键名获取配置
   */
  getConfigByKey: (configKey: string) => {
    return httpClient.get<ApiResponse<any>>(`/configs/key/${configKey}`)
  },

  /**
   * 创建配置
   */
  createConfig: (data: any) => {
    return httpClient.post<ApiResponse<any>>('/configs', data)
  },

  /**
   * 批量保存配置
   */
  batchSaveConfigs: (data: any[]) => {
    return httpClient.post<ApiResponse<any>>('/configs/batch', data)
  },

  /**
   * 更新配置
   */
  updateConfig: (id: number, data: any) => {
    return httpClient.put<ApiResponse<any>>(`/configs/${id}`, data)
  },

  /**
   * 删除配置
   */
  deleteConfig: (id: number) => {
    return httpClient.delete<ApiResponse<any>>(`/configs/${id}`)
  },

  /**
   * 批量删除配置
   */
  batchDeleteConfigs: (ids: number[]) => {
    return httpClient.post<ApiResponse<any>>('/configs/batch-delete', ids)
  }
}

export default configApi
