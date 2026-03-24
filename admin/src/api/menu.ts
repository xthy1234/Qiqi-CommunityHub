// src/api/menu.ts
import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'

/**
 * 菜单项接口
 */
export interface MenuItem {
  id: number
  name: string
  path?: string
  icon?: string
  parentId?: number
  sort?: number
  status?: number
  menuType?: number
  component?: string
  buttons?: string[]
  children?: MenuItem[]
  createTime?: string
  updateTime?: string
}

/**
 * 角色菜单权限接口
 */
export interface RoleMenu {
  roleId: number
  menuId: number
  buttons?: string[]
}

/**
 * 菜单 API 服务类
 */
class MenuService {
  private baseUrl = '/menus'

  /**
   * 获取所有菜单列表（无分页）
   */
  async getAllMenus(): Promise<MenuItem[]> {
    const response: AxiosResponse<ApiResponse<MenuItem[]>> = await httpClient.get(`${this.baseUrl}/all`)
    return response.data.data
  }

  /**
   * 获取菜单树结构
   */
  async getMenuTree(): Promise<MenuItem[]> {
    const response: AxiosResponse<ApiResponse<MenuItem[]>> = await httpClient.get(`${this.baseUrl}/tree`)
    return response.data.data
  }

  /**
   * 获取子菜单
   * @param parentId 父菜单 ID
   */
  async getChildren(parentId: number): Promise<MenuItem[]> {
    const response: AxiosResponse<ApiResponse<MenuItem[]>> = await httpClient.get(`${this.baseUrl}/${parentId}/children`)
    return response.data.data
  }

  /**
   * 获取菜单详情
   * @param id 菜单 ID
   */
  async getMenuById(id: number): Promise<MenuItem> {
    const response: AxiosResponse<ApiResponse<MenuItem>> = await httpClient.get(`${this.baseUrl}/${id}`)
    return response.data.data
  }

  /**
   * 验证菜单配置
   * @param data 菜单数据
   */
  async validateMenu(data: Partial<MenuItem>): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`${this.baseUrl}/validate`, data)
    return response.data.data
  }

  /**
   * 创建菜单
   * @param data 菜单数据
   */
  async createMenu(data: MenuItem): Promise<MenuItem> {
    const response: AxiosResponse<ApiResponse<MenuItem>> = await httpClient.post(this.baseUrl, data)
    return response.data.data
  }

  /**
   * 更新菜单
   * @param id 菜单 ID
   * @param data 菜单数据
   */
  async updateMenu(id: number, data: MenuItem): Promise<MenuItem> {
    const response: AxiosResponse<ApiResponse<MenuItem>> = await httpClient.put(`${this.baseUrl}/${id}`, data)
    return response.data.data
  }

  /**
   * 部分更新菜单
   * @param id 菜单 ID
   * @param data 菜单数据
   */
  async partialUpdateMenu(id: number, data: Partial<MenuItem>): Promise<MenuItem> {
    const response: AxiosResponse<ApiResponse<MenuItem>> = await httpClient.patch(`${this.baseUrl}/${id}`, data)
    return response.data.data
  }

  /**
   * 更新菜单状态
   * @param id 菜单 ID
   * @param status 状态
   */
  async updateStatus(id: number, status: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.patch(`${this.baseUrl}/${id}/status`, { status })
    return response.data.data
  }

  /**
   * 删除菜单
   * @param id 菜单 ID
   */
  async deleteMenu(id: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`${this.baseUrl}/${id}`)
    return response.data.data
  }

  /**
   * 批量删除菜单
   * @param ids 菜单 ID 数组
   */
  async batchDeleteMenus(ids: number[]): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(this.baseUrl, { data: { ids } })
    return response.data.data
  }

  /**
   * 统计菜单数量
   */
  async count(): Promise<number> {
    const response: AxiosResponse<ApiResponse<number>> = await httpClient.get(`${this.baseUrl}/count`)
    return response.data.data
  }

  /**
   * 获取当前用户角色的菜单权限
   */
  async getAuthMenus(): Promise<MenuItem[]> {
    const response: AxiosResponse<ApiResponse<MenuItem[]>> = await httpClient.get(`${this.baseUrl}/auth`)
    return response.data.data
  }

  /**
   * 获取菜单分页列表
   * @param params 查询参数
   */
  async getMenuList(params: { page: number; limit: number; [key: string]: any }): Promise<{ list: MenuItem[]; total: number }> {
    const response: AxiosResponse<ApiResponse<{ list: MenuItem[]; total: number }>> = await httpClient.get(this.baseUrl, { params })
    return response.data.data
  }
}

export default new MenuService()
