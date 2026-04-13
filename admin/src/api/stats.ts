// src/api/stats.ts

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
 * 仪表盘统计数据
 */
export interface DashboardStats {
  totalUsers: number
  todayNewUsers: number
  yesterdayNewUsers: number
  userGrowthRate: number
  totalArticles: number
  todayNewArticles: number
  yesterdayNewArticles: number
  articleGrowthRate: number
  pendingAuditArticles: number
  pendingReports: number
  totalComments: number
  todayActiveUsers: number
}

/**
 * 趋势数据点
 */
export interface TrendDataPoint {
  date: string
  count: number
}

/**
 * 获取仪表盘核心指标
 */
export function getDashboardStats(): Promise<ApiResponse<DashboardStats>> {
  return httpClient.get('/stats/dashboard')
}

/**
 * 获取文章审核概览统计
 */
export interface AuditOverviewStats {
  pendingCount: number
  todayApproved: number
  todayRejected: number
  totalCount: number
}

export function getAuditOverview(): Promise<ApiResponse<AuditOverviewStats>> {
  return httpClient.get('/stats/audit-overview')
}

/**
 * 获取用户增长趋势
 */
export function getUserGrowthTrend(days = 7): Promise<ApiResponse<TrendDataPoint[]>> {
  return httpClient.get('/stats/user-growth-trend', { params: { days } })
}

/**
 * 获取内容发布趋势
 */
export function getContentPublishTrend(days = 7): Promise<ApiResponse<TrendDataPoint[]>> {
  return httpClient.get('/stats/content-publish-trend', { params: { days } })
}

/**
 * 获取互动趋势
 */
export function getInteractionTrend(days = 7): Promise<ApiResponse<TrendDataPoint[]>> {
  return httpClient.get('/stats/interaction-trend', { params: { days } })
}

export default {
  getDashboardStats,
  getAuditOverview,
  getUserGrowthTrend,
  getContentPublishTrend,
  getInteractionTrend
}
