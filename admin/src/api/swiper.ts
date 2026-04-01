// src/api/swiper.ts
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
  pageNum: number
}

/**
 * Swiper 实体
 */
export interface Swiper {
  id?: number
  title: string
  imageUrl: string
  linkUrl?: string
  sort?: number
  status?: number | string // 修复：支持数字和字符串两种格式
  description?: string
  createTime?: string
  updateTime?: string
}

/**
 * SwiperCreateDTO - 创建轮播图请求
 */
export interface SwiperCreateDTO {
  title: string
  imageUrl: string
  linkUrl?: string
  sort?: number
  status?: number | string // 修复：支持字符串格式
  description?: string
}

/**
 * SwiperUpdateDTO - 更新轮播图请求
 */
export interface SwiperUpdateDTO {
  title?: string
  imageUrl?: string
  linkUrl?: string
  sort?: number
  status?: number | string // 修复：支持字符串格式
  description?: string
}

/**
 * SwiperVO - 轮播图视图对象
 */
export interface SwiperVO {
  id: number
  title: string
  imageUrl: string
  linkUrl?: string
  sort: number
  status: number | string // 修复：支持字符串格式
  description?: string
  createTime: string
  updateTime?: string
}

/**
 * 轮播图管理 API
 */
export const swiperApi = {
  /**
   * 分页查询轮播图列表
   */
  getSwiperList: (params: PageParams & { 
    title?: string,
    status?: number
  }) => {
    return httpClient.get<ApiResponse<PageResponse<SwiperVO>>>('/swipers', { params })
  },

  /**
   * 获取轮播图详情
   */
  getSwiperById: (id: number) => {
    return httpClient.get<ApiResponse<SwiperVO>>(`/swipers/${id}`)
  },

  /**
   * 获取启用的轮播图（无需认证）
   */
  getEnabledSwipers: () => {
    return httpClient.get<ApiResponse<SwiperVO[]>>('/swipers/enabled')
  },

  /**
   * 统计轮播图总数
   */
  countSwipers: (params?: { title?: string; status?: number }) => {
    return httpClient.get<ApiResponse<number>>('/swipers/count', { params })
  },

  /**
   * 创建轮播图
   */
  createSwiper: (data: SwiperCreateDTO) => {
    return httpClient.post<ApiResponse<Swiper>>('/swipers', data)
  },

  /**
   * 全量更新轮播图
   */
  updateSwiper: (id: number, data: SwiperUpdateDTO) => {
    return httpClient.put<ApiResponse<Swiper>>(`/swipers/${id}`, data)
  },

  /**
   * 部分更新轮播图
   */
  patchSwiper: (id: number, data: Partial<SwiperUpdateDTO>) => {
    return httpClient.patch<ApiResponse<Swiper>>(`/swipers/${id}`, data)
  },

  /**
   * 更新轮播图状态
   */
  updateSwiperStatus: (id: number, status: number) => {
    return httpClient.patch<ApiResponse<Swiper>>(`/swipers/${id}/status`, null, {
      params: { status }
    })
  },

  /**
   * 删除单个轮播图
   */
  deleteSwiper: (id: number) => {
    return httpClient.delete<ApiResponse<void>>(`/swipers/${id}`)
  },

  /**
   * 批量删除轮播图
   */
  batchDeleteSwipers: (ids: number[]) => {
    return httpClient.post<ApiResponse<void>>('/swipers/batch-delete', ids)
  }
}

export default {
  swiper: swiperApi
}
