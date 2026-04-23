// src/api/menu.ts
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
 * Menu 实体
 */
export interface Menu {
  id?: number
  parentId: number | null
  name: string
  path: string
  type: number
  icon?: string
  sortOrder: number
  status: number
  buttons?: string[]
  component?: string
  children?: Menu[]
}

/**
 * MenuItem 类型（用于 utils/menu.ts）
 */
export type MenuItem = Menu

/**
 * 菜单管理 API
 */
export const menuApi = {
  /**
   * 获取菜单分页列表
   */
  getMenuList: (params: PageParams & { menu?: Partial<Menu> }) => {
    return httpClient.get<ApiResponse<PageResponse<Menu>>>('/menus', { params })
  },

  /**
   * 获取所有菜单列表（无分页）
   */
  getAllMenus: (menu?: Partial<Menu>) => {
    return httpClient.get<ApiResponse<Menu[]>>('/menus/all', { params: menu })
  },

  /**
   * 获取菜单详情
   */
  getMenuById: (id: number) => {
    return httpClient.get<ApiResponse<Menu>>(`/menus/${id}`)
  },

  /**
   * 获取菜单树结构
   */
  getMenuTree: () => {
    return httpClient.get<ApiResponse<Menu[]>>('/menus/tree')
  },

  /**
   * 获取当前用户菜单权限
   */
  getAuthMenus: () => {
    return httpClient.get<ApiResponse<Menu[]>>('/menus/auth')
  },

  /**
   * 获取子菜单
   */
  getChildrenMenus: (parentId: number) => {
    return httpClient.get<ApiResponse<Menu[]>>(`/menus/${parentId}/children`)
  },

  /**
   * 统计菜单数量
   */
  countMenus: (menuType?: number, status?: number) => {
    return httpClient.get<ApiResponse<number>>('/menus/count', {
      params: { menuType, status }
    })
  },

  /**
   * 创建菜单
   */
  createMenu: (data: Menu) => {
    return httpClient.post<ApiResponse<Menu>>('/menus', data)
  },

  /**
   * 更新菜单信息
   */
  updateMenu: (id: number, data: Menu) => {
    return httpClient.put<ApiResponse<Menu>>(`/menus/${id}`, data)
  },

  /**
   * 部分更新菜单
   */
  patchMenu: (id: number, data: Partial<Menu>) => {
    return httpClient.patch<ApiResponse<Menu>>(`/menus/${id}`, data)
  },

  /**
   * 更新菜单状态
   */
  updateMenuStatus: (id: number, status: number) => {
    return httpClient.patch<ApiResponse<Menu>>(`/menus/${id}/status`, { status })
  },

  /**
   * 删除单个菜单
   */
  deleteMenu: (id: number) => {
    return httpClient.delete<ApiResponse<void>>(`/menus/${id}`)
  },

  /**
   * 批量删除菜单
   */
  batchDeleteMenus: (ids: number[]) => {
    return httpClient.delete<ApiResponse<void>>('/menus', { data: ids })
  }
}

export default menuApi
