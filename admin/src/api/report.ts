// src/api/report.ts
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
 * 审核状态枚举
 */
export enum ReviewStatus {
  PENDING = 0,    // 待审核
  APPROVED = 1,   // 已通过
  REJECTED = 2    // 已拒绝
}

/**
 * 处理动作枚举
 */
export enum ReportAction {
  BLOCK = 'BLOCK',      // 屏蔽文章（作者可见但不可传播）
  DELETE = 'DELETE',    // 删除文章（软删除）
  WARN = 'WARN',        // 仅警告（不修改文章状态）
  IGNORE = 'IGNORE'     // 忽略举报（不处理文章）
}

/**
 * Report 实体
 */
export interface Report {
  id?: number
  reporterId?: number
  reporterName?: string
  reportedId?: number
  reportedName?: string
  contentType?: string
  contentTitle?: string
  reportReason?: string
  reportDesc?: string
  reviewStatus?: ReviewStatus
  replyContent?: string
  reviewerId?: number
  reviewerName?: string
  reviewTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * ReportCreateDTO - 创建举报请求
 */
export interface ReportCreateDTO {
  reportedId: number
  contentType: string
  reportReason: string
  reportDesc?: string
}

/**
 * ReportReviewDTO - 审核举报请求（旧接口，保留兼容）
 */
export interface ReportReviewDTO {
  reviewStatus: ReviewStatus
  replyContent?: string
}

/**
 * ReportReviewWithActionDTO - 审核举报并执行处理动作（新接口）
 */
export interface ReportReviewWithActionDTO {
  reportId?: number
  reviewStatus: ReviewStatus
  replyContent: string
  action: ReportAction
  rewardReporter?: boolean
  penalizeReportedUser?: boolean
}

/**
 * ReportBatchReviewDTO - 批量审核举报请求
 */
export interface ReportBatchReviewDTO {
  reportIds: number[]
  reviewStatus: ReviewStatus
  replyContent?: string
}

/**
 * 用户信息对象（用于举报中的举报人和被举报人）
 */
export interface UserInfo {
  id: number
  nickname: string
  avatar?: string
  lastOnlineTime?: string | null
}

/**
 * ReportVO - 举报视图对象
 */
export interface ReportVO {
  id: number
  reporterId: number
  reporterName?: string
  reportedId: number
  reportedName?: string
  reporterUserInfo?: UserInfo
  reportedUserInfo?: UserInfo
  reviewUserInfo?: UserInfo | null
  contentType?: string
  contentTitle?: string
  reportReason: string
  reportDesc?: string
  reviewStatus: ReviewStatus
  replyContent?: string
  reviewerId?: number
  reviewerName?: string
  reviewTime?: string
  createTime: string
  updateTime?: string
  status?: number
  contentId?: number
  reportTime?: string
}

/**
 * 举报管理 API
 */
export const reportApi = {
  /**
   * 获取举报分页列表
   */
  getReportList: (params: PageParams & { 
    reviewStatus?: ReviewStatus,
    startDate?: string,
    endDate?: string
  }) => {
    return httpClient.get<ApiResponse<PageResponse<ReportVO>>>('/reports', { params })
  },

  /**
   * 获取举报详情
   */
  getReportById: (id: number) => {
    return httpClient.get<ApiResponse<ReportVO>>(`/reports/${id}`)
  },

  /**
   * 查看某个用户提交的所有举报记录
   */
  getUserReports: (userId: number, params?: PageParams) => {
    return httpClient.get<ApiResponse<PageResponse<ReportVO>>>(`/reports/user/${userId}`, { params })
  },

  /**
   * 创建举报
   */
  createReport: (data: ReportCreateDTO) => {
    return httpClient.post<ApiResponse<Report>>('/reports', data)
  },

  /**
   * 审核举报（单条，旧接口，保留兼容）
   */
  reviewReport: (id: number, data: ReportReviewDTO) => {
    return httpClient.post<ApiResponse<void>>(`/reports/${id}/review`, data)
  },

  /**
   * 审核举报并执行处理动作（新接口，推荐使用）
   */
  reviewReportWithAction: (id: number, data: ReportReviewWithActionDTO) => {
    return httpClient.post<ApiResponse<void>>(`/reports/${id}/review-with-action`, data)
  },

  /**
   * 批量审核举报
   */
  batchReviewReports: (data: ReportBatchReviewDTO) => {
    return httpClient.post<ApiResponse<void>>('/reports/batch-review', data)
  },

  /**
   * 按审核状态统计举报数量
   */
  countByStatus: (reviewStatus: ReviewStatus) => {
    return httpClient.get<ApiResponse<number>>(`/reports/stats/status/${reviewStatus}`)
  },

  /**
   * 统计举报总数（支持条件筛选）
   */
  countReports: (params?: { reviewStatus?: ReviewStatus; startDate?: string; endDate?: string }) => {
    return httpClient.get<ApiResponse<number>>('/reports/count', { params })
  },

  /**
   * 全量更新举报信息
   */
  updateReport: (id: number, data: Partial<Report>) => {
    return httpClient.put<ApiResponse<Report>>(`/reports/${id}`, data)
  },

  /**
   * 部分更新举报信息
   */
  patchReport: (id: number, data: Partial<Report>) => {
    return httpClient.patch<ApiResponse<Report>>(`/reports/${id}`, data)
  },

  /**
   * 删除单个举报
   */
  deleteReport: (id: number) => {
    return httpClient.delete<ApiResponse<void>>(`/reports/${id}`)
  },

  /**
   * 批量删除举报
   */
  batchDeleteReports: (ids: number[]) => {
    return httpClient.post<ApiResponse<void>>('/reports/batch-delete', ids)
  }
}

export default {
  report: reportApi
}
