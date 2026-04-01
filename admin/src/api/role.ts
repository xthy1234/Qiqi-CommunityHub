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
 * Menu 实体
 */
export interface Menu {
  id?: number
  parentId: number
  name: string
  path: string
  type: number
  icon?: string
  sortOrder: number
  status: number
  buttons?: string[]
  children?: Menu[]
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

export default {
  role: roleApi,
  menu: menuApi,
  roleMenu: roleMenuApi
}
