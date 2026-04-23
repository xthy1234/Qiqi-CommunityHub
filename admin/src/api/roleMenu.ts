// src/api/roleMenu.ts
import httpClient from '@/utils/http'

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

/**
 * RoleMenu 关联实体
 */
export interface RoleMenu {
  roleId: number
  menuId: number
  buttons?: string[]
}

/**
 * 角色菜单权限 API
 */
export const roleMenuApi = {
  /**
   * 获取角色的菜单权限列表
   */
  getMenusByRole: (roleId: number) => {
    return httpClient.get<ApiResponse<RoleMenu[]>>(`/role-menus/by-role/${roleId}`)
  },

  /**
   * 获取菜单的角色权限列表
   */
  getRolesByMenu: (menuId: number) => {
    return httpClient.get<ApiResponse<RoleMenu[]>>(`/role-menus/by-menu/${menuId}`)
  },

  /**
   * 保存角色的菜单权限
   */
  saveRoleMenus: (roleId: number, menuIds: number[]) => {
    return httpClient.post<ApiResponse<void>>(`/role-menus/by-role/${roleId}`, menuIds)
  },

  /**
   * 批量保存角色菜单权限（含按钮权限）
   */
  batchSaveRoleMenus: (data: RoleMenu[]) => {
    return httpClient.post<ApiResponse<void>>('/role-menus', data)
  },

  /**
   * 删除角色的所有菜单权限
   */
  deleteRoleMenus: (roleId: number) => {
    return httpClient.delete<ApiResponse<void>>(`/role-menus/by-role/${roleId}`)
  },

  /**
   * 删除角色的单个菜单权限
   */
  deleteRoleMenu: (roleId: number, menuId: number) => {
    return httpClient.delete<ApiResponse<void>>(`/role-menus/by-role/${roleId}/menu/${menuId}`)
  }
}

export default roleMenuApi
