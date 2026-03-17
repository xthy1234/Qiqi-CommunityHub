// src/api/index.ts

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
    total: number
}

/**
 * API 服务类
 * 统一管理所有后端接口调用
 */
class ApiService {
    /**
     * 验证码相关接口
     */
    captcha = {
        /**
         * 获取图形验证码
         */
        getCaptcha: () => {
            return httpClient.get<ApiResponse<{ captchaKey: string; captchaImage: string }>>('/captcha/image')
        }
    };

    /**
     * 用户相关接口
     */
    user = {
        /**
         * 管理员登录
         * @param data 登录信息 {account: 账号，password: 密码，captcha: 验证码}
         */
        adminLogin: (data: { account: string; password: string; captcha?: string }) => {
            return httpClient.post<ApiResponse<{ token: string; user: any }>>('/users/admin/login', data)
        },

        /**
         * 管理员注册
         */
        adminRegister: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/users/admin/register', data)
        },

        /**
         * 用户登录
         */
        login: (data: { username: string; password: string }) => {
            return httpClient.post<ApiResponse<{ token: string; user: any }>>('/users/login', data)
        },

        /**
         * 用户注册
         */
        register: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/users/register', data)
        },

        /**
         * 退出登录
         */
        logout: () => {
            return httpClient.post<ApiResponse>('/users/logout')
        },

        /**
         * 获取当前用户信息
         */
        getCurrentUser: () => {
            return httpClient.get<ApiResponse<any>>('/users/me')
        },

        /**
         * 获取用户列表
         */
        getUserList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/users', { params })
        },

        /**
         * 获取用户详情
         */
        getUserById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/users/${id}`)
        },

        /**
         * 创建用户
         */
        createUser: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/users', data)
        },

        /**
         * 更新用户
         */
        updateUser: (id: number, data: any) => {
            return httpClient.put<ApiResponse<any>>(`/users/${id}`, data)
        },

        /**
         * 删除用户
         */
        deleteUser: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/users/${id}`)
        },

        /**
         * 批量删除用户
         */
        batchDeleteUsers: (ids: number[]) => {
            return httpClient.post<ApiResponse<any>>('/users/batch-delete', ids)
        },

        /**
         * 更新用户密码
         */
        updatePassword: (id: number, data: { oldPassword: string; newPassword: string }) => {
            return httpClient.put<ApiResponse<any>>(`/users/${id}/password`, data)
        },

        /**
         * 重置密码
         */
        resetPassword: (data: { username: string; newPassword: string }) => {
            return httpClient.post<ApiResponse<any>>('/users/reset-password', data)
        }
    }

    /**
     * 菜单相关接口
     */
    menu = {
        /**
         * 获取所有菜单（无分页）
         */
        getAllMenus: () => {
            return httpClient.get<ApiResponse<any[]>>('/menus/all')
        },

        /**
         * 获取菜单列表（分页）
         */
        getMenuList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/menus', { params })
        },

        /**
         * 获取菜单树结构
         */
        getMenuTree: () => {
            return httpClient.get<ApiResponse<any[]>>('/menus/tree')
        },

        /**
         * 获取子菜单
         */
        getChildrenMenus: (parentId: number) => {
            return httpClient.get<ApiResponse<any[]>>(`/menus/${parentId}/children`)
        },

        /**
         * 创建菜单
         */
        createMenu: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/menus', data)
        },

        /**
         * 更新菜单
         */
        updateMenu: (id: number, data: any) => {
            return httpClient.put<ApiResponse<any>>(`/menus/${id}`, data)
        },

        /**
         * 删除菜单
         */
        deleteMenu: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/menus/${id}`)
        },

        /**
         * 批量删除菜单
         */
        batchDeleteMenus: (ids: number[]) => {
            return httpClient.delete<ApiResponse<any>>('/menus', { data: ids })
        }
    }

    /**
     * 文章相关接口
     */
    article = {
        /**
         * 获取文章列表
         */
        getArticleList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/articles', { params })
        },

        /**
         * 获取文章详情
         */
        getArticleById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/articles/${id}`)
        },

        /**
         * 创建文章
         */
        createArticle: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/articles', data)
        },

        /**
         * 更新文章
         */
        updateArticle: (id: number, data: any) => {
            return httpClient.put<ApiResponse<any>>(`/articles/${id}`, data)
        },

        /**
         * 删除文章
         */
        deleteArticle: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/articles/${id}`)
        },

        /**
         * 批量审核文章
         */
        batchAuditArticles: (data: { ids: number[]; status: string }) => {
            return httpClient.post<ApiResponse<any>>('/articles/batch-audit', data)
        },

        /**
         * 批量删除文章
         */
        batchDeleteArticles: (ids: number[]) => {
            return httpClient.post<ApiResponse<any>>('/articles/batch-delete', ids)
        },

        /**
         * 获取文章总数
         */
        getArticleCount: () => {
            return httpClient.get<ApiResponse<number>>('/articles/count')
        },

        /**
         * 按字段分组统计
         */
        getStatsByColumn: (column: string) => {
            return httpClient.get<ApiResponse<any[]>>(`/articles/stats/group/${column}`)
        },

        /**
         * 按时间维度统计
         */
        getStatsByTime: (xColumn: string, yColumn: string, timeType: string) => {
            return httpClient.get<ApiResponse<any[]>>(`/articles/stats/time/${xColumn}/${yColumn}/${timeType}`)
        }
    }

    /**
     * 分类相关接口
     */
    category = {
        /**
         * 获取分类列表
         */
        getCategoryList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/categories', { params })
        },

        /**
         * 获取分类详情
         */
        getCategoryById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/categories/${id}`)
        },

        /**
         * 创建分类
         */
        createCategory: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/categories', data)
        },

        /**
         * 更新分类
         */
        updateCategory: (id: number, data: any) => {
            return httpClient.put<ApiResponse<any>>(`/categories/${id}`, data)
        },

        /**
         * 删除分类
         */
        deleteCategory: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/categories/${id}`)
        },

        /**
         * 批量删除分类
         */
        batchDeleteCategories: (ids: number[]) => {
            return httpClient.post<ApiResponse<any>>('/categories/batch-delete', ids)
        },

        /**
         * 获取所有启用的分类
         */
        getEnabledCategories: () => {
            return httpClient.get<ApiResponse<any[]>>('/categories/enabled')
        },

        /**
         * 获取分类树
         */
        getCategoryTree: () => {
            return httpClient.get<ApiResponse<any[]>>('/categories/tree')
        }
    }

    /**
     * 评论相关接口
     */
    comment = {
        /**
         * 获取评论列表
         */
        getCommentList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/comments', { params })
        },

        /**
         * 获取评论详情
         */
        getCommentById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/comments/${id}`)
        },

        /**
         * 根据内容 ID 获取评论
         */
        getCommentsByContentId: (contentId: number) => {
            return httpClient.get<ApiResponse<any[]>>(`/comments/content/${contentId}`)
        },

        /**
         * 创建评论
         */
        createComment: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/comments', data)
        },

        /**
         * 更新评论
         */
        updateComment: (id: number, data: any) => {
            return httpClient.put<ApiResponse<any>>(`/comments/${id}`, data)
        },

        /**
         * 删除评论
         */
        deleteComment: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/comments/${id}`)
        },

        /**
         * 批量删除评论
         */
        batchDeleteComments: (ids: number[]) => {
            return httpClient.post<ApiResponse<any>>('/comments/batch-delete', ids)
        }
    }

    /**
     * 举报相关接口
     */
    report = {
        /**
         * 获取举报列表
         */
        getReportList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/reports', { params })
        },

        /**
         * 获取举报详情
         */
        getReportById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/reports/${id}`)
        },

        /**
         * 创建举报
         */
        createReport: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/reports', data)
        },

        /**
         * 审核举报
         */
        reviewReport: (id: number, data: any) => {
            return httpClient.post<ApiResponse<any>>(`/reports/${id}/review`, data)
        },

        /**
         * 批量审核举报
         */
        batchReviewReports: (data: { ids: number[]; status: string }) => {
            return httpClient.post<ApiResponse<any>>('/reports/batch-review', data)
        },

        /**
         * 删除举报
         */
        deleteReport: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/reports/${id}`)
        },

        /**
         * 批量删除举报
         */
        batchDeleteReports: (ids: number[]) => {
            return httpClient.post<ApiResponse<any>>('/reports/batch-delete', ids)
        }
    }

    /**
     * 反馈相关接口
     */
    feedback = {
        /**
         * 获取反馈列表
         */
        getFeedbackList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/feedbacks', { params })
        },

        /**
         * 获取反馈详情
         */
        getFeedbackById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/feedbacks/${id}`)
        },

        /**
         * 创建反馈
         */
        createFeedback: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/feedbacks', data)
        },

        /**
         * 回复反馈
         */
        replyFeedback: (id: number, data: { reply: string }) => {
            return httpClient.post<ApiResponse<any>>(`/feedbacks/${id}/reply`, data)
        },

        /**
         * 更新反馈
         */
        updateFeedback: (id: number, data: any) => {
            return httpClient.put<ApiResponse<any>>(`/feedbacks/${id}`, data)
        },

        /**
         * 删除反馈
         */
        deleteFeedback: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/feedbacks/${id}`)
        },

        /**
         * 批量删除反馈
         */
        batchDeleteFeedbacks: (ids: number[]) => {
            return httpClient.post<ApiResponse<any>>('/feedbacks/batch-delete', ids)
        }
    }

    /**
     * 互动相关接口（点赞、收藏等）
     */
    interaction = {
        /**
         * 获取互动列表
         */
        getInteractionList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/interactions', { params })
        },

        /**
         * 获取互动详情
         */
        getInteractionById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/interactions/${id}`)
        },

        /**
         * 添加互动
         */
        createInteraction: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/interactions', data)
        },

        /**
         * 点赞/点踩
         */
        like: (data: { type: string; targetId: number; action: 'like' | 'dislike' }) => {
            return httpClient.post<ApiResponse<any>>('/interactions/like', data)
        },

        /**
         * 取消点赞
         */
        unlike: (data: { type: string; targetId: number }) => {
            return httpClient.delete<ApiResponse<any>>('/interactions/like', { data })
        },

        /**
         * 取消互动
         */
        cancelInteraction: (data: any) => {
            return httpClient.delete<ApiResponse<any>>('/interactions/action', { data })
        },

        /**
         * 删除互动
         */
        deleteInteraction: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/interactions/${id}`)
        },

        /**
         * 批量删除互动
         */
        batchDeleteInteractions: (ids: number[]) => {
            return httpClient.post<ApiResponse<any>>('/interactions/batch-delete', ids)
        }
    }

    /**
     * 配置相关接口
     */
    config = {
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

    /**
     * 角色相关接口
     */
    role = {
        /**
         * 获取角色列表（后台）
         */
        getRoleList: (params: PageParams) => {
            return httpClient.get<ApiResponse<PageResponse<any>>>('/roles', { params })
        },

        /**
         * 获取所有角色（前端）
         */
        getAllRoles: () => {
            return httpClient.get<ApiResponse<any[]>>('/roles/all')
        },

        /**
         * 获取角色详情
         */
        getRoleById: (id: number) => {
            return httpClient.get<ApiResponse<any>>(`/roles/${id}`)
        },

        /**
         * 创建角色
         */
        createRole: (data: any) => {
            return httpClient.post<ApiResponse<any>>('/roles', data)
        },

        /**
         * 更新角色
         */
        updateRole: (id: number, data: any) => {
            return httpClient.put<ApiResponse<any>>(`/roles/${id}`, data)
        },

        /**
         * 删除角色
         */
        deleteRole: (id: number) => {
            return httpClient.delete<ApiResponse<any>>(`/roles/${id}`)
        },

        /**
         * 批量删除角色
         */
        batchDeleteRoles: (ids: number[]) => {
            return httpClient.delete<ApiResponse<any>>('/roles', { data: ids })
        }
    }

    /**
     * 角色菜单权限相关接口
     */
    roleMenu = {
        /**
         * 获取角色的菜单权限
         */
        getMenusByRole: (roleId: number) => {
            return httpClient.get<ApiResponse<any[]>>(`/role-menus/by-role/${roleId}`)
        },

        /**
         * 获取菜单的角色权限
         */
        getRolesByMenu: (menuId: number) => {
            return httpClient.get<ApiResponse<any[]>>(`/role-menus/by-menu/${menuId}`)
        },

        /**
         * 保存角色的菜单权限
         */
        saveRoleMenus: (roleId: number, data: any[]) => {
            return httpClient.post<ApiResponse<any>>(`/role-menus/by-role/${roleId}`, data)
        },

        /**
         * 批量保存角色菜单权限
         */
        batchSaveRoleMenus: (data: any[]) => {
            return httpClient.post<ApiResponse<any>>('/role-menus', data)
        },

        /**
         * 删除角色的所有菜单权限
         */
        deleteRoleMenus: (roleId: number) => {
            return httpClient.delete<ApiResponse<any>>(`/role-menus/by-role/${roleId}`)
        },

        /**
         * 删除角色的单个菜单权限
         */
        deleteRoleMenu: (roleId: number, menuId: number) => {
            return httpClient.delete<ApiResponse<any>>(`/role-menus/by-role/${roleId}/menu/${menuId}`)
        }
    }
}

// 导出单例
export const apiService = new ApiService()
export default apiService
