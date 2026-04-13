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
    const response = await httpClient.get(`${this.baseUrl}/all`)
    return response.data
  }

  /**
   * 获取菜单树结构
   */
  async getMenuTree(): Promise<MenuItem[]> {
    const response = await httpClient.get(`${this.baseUrl}/tree`)
    return response.data
  }

  /**
   * 获取子菜单
   */
  async getChildren(parentId: number): Promise<MenuItem[]> {
    const response = await httpClient.get(`${this.baseUrl}/${parentId}/children`)
    return response.data
  }

  /**
   * 根据ID获取菜单详情
   */
  async getMenuById(id: number): Promise<MenuItem> {
    const response = await httpClient.get(`${this.baseUrl}/${id}`)
    return response.data
  }

  /**
   * 验证菜单数据
   */
  async validateMenu(data: Partial<MenuItem>): Promise<void> {
    await httpClient.post(`${this.baseUrl}/validate`, data)
  }

  /**
   * 创建菜单
   */
  async createMenu(data: MenuItem): Promise<MenuItem> {
    const response = await httpClient.post(this.baseUrl, data)
    return response.data
  }

  /**
   * 更新菜单（全量）
   */
  async updateMenu(id: number, data: MenuItem): Promise<MenuItem> {
    const response = await httpClient.put(`${this.baseUrl}/${id}`, data)
    return response.data
  }

  /**
   * 部分更新菜单
   */
  async partialUpdateMenu(id: number, data: Partial<MenuItem>): Promise<MenuItem> {
    const response = await httpClient.patch(`${this.baseUrl}/${id}`, data)
    return response.data
  }

  /**
   * 更新菜单状态
   */
  async updateStatus(id: number, status: number): Promise<void> {
    await httpClient.patch(`${this.baseUrl}/${id}/status`, { status })
  }

  /**
   * 删除菜单
   */
  async deleteMenu(id: number): Promise<void> {
    await httpClient.delete(`${this.baseUrl}/${id}`)
  }

  /**
   * 批量删除菜单
   */
  async batchDeleteMenus(ids: number[]): Promise<void> {
    await httpClient.delete(this.baseUrl, { data: { ids } })
  }

  /**
   * 获取菜单总数
   */
  async count(): Promise<number> {
    const response = await httpClient.get(`${this.baseUrl}/count`)
    return response.data
  }

  /**
   * 获取当前用户有权限的菜单
   */
  async getAuthMenus(): Promise<MenuItem[]> {
    const response = await httpClient.get(`${this.baseUrl}/auth`)
    return response.data
  }

  /**
   * 分页获取菜单列表
   */
  async getMenuList(params: { page: number; limit: number; [key: string]: any }): Promise<{ list: MenuItem[]; total: number }> {
    const response = await httpClient.get(this.baseUrl, { params })
    return response.data
  }
}

export default new MenuService()
