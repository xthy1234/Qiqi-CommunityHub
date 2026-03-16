// src/api/roleMenu.ts
import httpClient from '@/utils/http'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/utils/http'
import type { RoleMenu } from './menu'

/**
 * 角色菜单 API 服务类
 */
class RoleMenuService {
  private baseUrl = '/role-menus'

  /**
   * 获取角色的菜单权限列表
   * @param roleId 角色 ID
   */
  async getByRoleId(roleId: number): Promise<RoleMenu[]> {
    const response: AxiosResponse<ApiResponse<RoleMenu[]>> = await httpClient.get(`${this.baseUrl}/by-role/${roleId}`)
    return response.data.data
  }

  /**
   * 获取菜单的角色权限列表
   * @param menuId 菜单 ID
   */
  async getByMenuId(menuId: number): Promise<RoleMenu[]> {
    const response: AxiosResponse<ApiResponse<RoleMenu[]>> = await httpClient.get(`${this.baseUrl}/by-menu/${menuId}`)
    return response.data.data
  }

  /**
   * 保存角色的菜单权限
   * @param roleId 角色 ID
   * @param menuIds 菜单 ID 数组
   */
  async saveByRoleId(roleId: number, menuIds: number[]): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(`${this.baseUrl}/by-role/${roleId}`, { menuIds })
    return response.data.data
  }

  /**
   * 批量保存角色菜单权限（包含按钮权限）
   * @param data 角色菜单权限数据
   */
  async batchSave(data: RoleMenu[]): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.post(this.baseUrl, data)
    return response.data.data
  }

  /**
   * 删除角色的所有菜单权限
   * @param roleId 角色 ID
   */
  async deleteByRoleId(roleId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`${this.baseUrl}/by-role/${roleId}`)
    return response.data.data
  }

  /**
   * 删除角色的单个菜单权限
   * @param roleId 角色 ID
   * @param menuId 菜单 ID
   */
  async deleteByRoleIdAndMenuId(roleId: number, menuId: number): Promise<void> {
    const response: AxiosResponse<ApiResponse<void>> = await httpClient.delete(`${this.baseUrl}/by-role/${roleId}/menu/${menuId}`)
    return response.data.data
  }
}

export default new RoleMenuService()
