// src/api/category.ts
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
 * Category 实体
 */
export interface Category {
  id?: number
  categoryName: string
  description?: string
  sort?: number
  status?: number // 0:启用，1:禁用
  parentId?: number
  createTime?: string
  updateTime?: string
}

/**
 * CategoryCreateDTO - 创建分类请求
 */
export interface CategoryCreateDTO {
  categoryName: string
  description?: string
  sort?: number
  parentId?: number
}

/**
 * CategoryUpdateDTO - 更新分类请求
 */
export interface CategoryUpdateDTO {
  categoryName?: string
  description?: string
  sort?: number
  status?: number
  parentId?: number
}

/**
 * CategoryVO - 分类视图对象
 */
export interface CategoryVO {
  id: number
  categoryName: string
  description?: string
  sort: number
  status: number
  createTime: string
  updateTime?: string
  children?: CategoryVO[]
}

/**
 * CategoryTreeVO - 分类树节点
 */
export interface CategoryTreeVO {
  id: number
  categoryName: string
  description?: string
  sort: number
  status: number
  parentId?: number
  children?: CategoryTreeVO[]
}

/**
 * 分类管理 API
 */
export const categoryApi = {
  /**
   * 获取分类分页列表
   */
  getCategoryList: (params: PageParams & { category?: Partial<Category> }) => {
    return httpClient.get<ApiResponse<PageResponse<CategoryVO>>>('/categories', { params })
  },

  /**
   * 获取分类详情
   */
  getCategoryById: (id: number) => {
    return httpClient.get<ApiResponse<CategoryDetailVO>>(`/categories/${id}`)
  },

  /**
   * 获取所有启用的分类（免登录）
   */
  getEnabledCategories: () => {
    return httpClient.get<ApiResponse<CategoryVO[]>>('/categories/enabled')
  },

  /**
   * 获取分类树结构
   */
  getCategoryTree: () => {
    return httpClient.get<ApiResponse<CategoryTreeVO[]>>('/categories/tree')
  },

  /**
   * 创建分类
   */
  createCategory: (data: CategoryCreateDTO) => {
    return httpClient.post<ApiResponse<Category>>('/categories', data)
  },

  /**
   * 更新分类信息（全量更新）
   */
  updateCategory: (id: number, data: CategoryUpdateDTO) => {
    return httpClient.put<ApiResponse<Category>>(`/categories/${id}`, data)
  },

  /**
   * 部分更新分类
   */
  patchCategory: (id: number, data: Partial<CategoryUpdateDTO>) => {
    return httpClient.patch<ApiResponse<Category>>(`/categories/${id}`, data)
  },

  /**
   * 启用/禁用分类
   */
  updateCategoryStatus: (id: number, status: number) => {
    return httpClient.patch<ApiResponse<Category>>(`/categories/${id}/status`, { status })
  },

  /**
   * 删除单个分类
   */
  deleteCategory: (id: number) => {
    return httpClient.delete<ApiResponse<void>>(`/categories/${id}`)
  },

  /**
   * 批量删除分类
   */
  batchDeleteCategories: (ids: number[]) => {
    return httpClient.post<ApiResponse<void>>('/categories/batch-delete', ids)
  }
}

/**
 * CategoryDetailVO - 分类详情
 */
interface CategoryDetailVO extends CategoryVO {
  updateTime: string
}

export default {
  category: categoryApi
}
