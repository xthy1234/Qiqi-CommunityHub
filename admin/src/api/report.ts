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
 * ReportReviewDTO - 审核举报请求
 */
export interface ReportReviewDTO {
  reviewStatus: ReviewStatus
  replyContent?: string
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
 * ReportVO - 举报视图对象
 */
export interface ReportVO {
  id: number
  reporterId: number
  reporterName: string
  reportedId: number
  reportedName: string
  contentType: string
  contentTitle: string
  reportReason: string
  reportDesc?: string
  reviewStatus: ReviewStatus
  replyContent?: string
  reviewerId?: number
  reviewerName?: string
  reviewTime?: string
  createTime: string
  updateTime?: string
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
   * 审核举报（单条）
   */
  reviewReport: (id: number, data: ReportReviewDTO) => {
    return httpClient.post<ApiResponse<void>>(`/reports/${id}/review`, data)
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
