// src/api/article.ts

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
 * 管理员查询文章列表
 */
export function getArticleList(params: PageParams): Promise<ApiResponse<PageResponse<any>>> {
  return httpClient.get('/articles/admin/list', { params })
}

/**
 * 管理员获取文章详情
 */
export function getArticleById(id: number): Promise<ApiResponse<any>> {
  return httpClient.get(`/articles/admin/${id}`)
}

/**
 * 创建文章
 */
export function createArticle(data: any): Promise<ApiResponse<any>> {
  return httpClient.post('/articles', data)
}

/**
 * 更新文章
 */
export function updateArticle(id: number, data: any): Promise<ApiResponse<any>> {
  return httpClient.put(`/articles/${id}`, data)
}

/**
 * 删除文章
 */
export function deleteArticle(id: number): Promise<ApiResponse<any>> {
  return httpClient.delete(`/articles/${id}`)
}

/**
 * 批量审核文章
 */
export function batchAuditArticles(data: { ids: number[]; status: number; reply?: string }): Promise<ApiResponse<any>> {
  return httpClient.post('/articles/admin/batch-audit', data)
}

/**
 * 批量修改文章分类
 */
export function batchUpdateCategory(data: { ids: number[]; categoryId: number }): Promise<ApiResponse<any>> {
  return httpClient.post('/articles/admin/batch-update-category', data)
}

/**
 * 批量删除文章
 */
export function batchDeleteArticles(ids: number[]): Promise<ApiResponse<any>> {
  return httpClient.post('/articles/batch-delete', ids)
}

/**
 * 获取文章总数
 */
export function getArticleCount(): Promise<ApiResponse<number>> {
  return httpClient.get('/articles/count')
}

/**
 * 按字段分组统计
 */
export function getStatsByColumn(column: string): Promise<ApiResponse<any[]>> {
  return httpClient.get(`/articles/stats/group/${column}`)
}

/**
 * 按时间维度统计
 */
export function getStatsByTime(xColumn: string, yColumn: string, timeType: string): Promise<ApiResponse<any[]>> {
  return httpClient.get(`/articles/stats/time/${xColumn}/${yColumn}/${timeType}`)
}

/**
 * 按值统计
 */
export function getStatsByValue(xColumn: string, yColumn: string): Promise<ApiResponse<any[]>> {
  return httpClient.get(`/articles/stats/value/${xColumn}/${yColumn}`)
}

/**
 * 多列统计
 */
export function getStatsMultiple(xColumn: string, yColumns: string[]): Promise<ApiResponse<any[]>> {
  return httpClient.get(`/articles/stats/value/multiple/${xColumn}`, {
    params: { yColumns: yColumns.join(',') }
  })
}

/**
 * 搜索文章
 */
export function searchArticles(params: { keyword: string; categoryId?: number; startDate?: string; endDate?: string; limit?: number }): Promise<ApiResponse<PageResponse<any>>> {
  return httpClient.get('/articles/search', { params })
}

/**
 * 获取文章版本列表
 */
export function getArticleVersions(articleId: number, params?: PageParams): Promise<ApiResponse<PageResponse<any>>> {
  return httpClient.get(`/articles/${articleId}/versions`, { params })
}

/**
 * 获取指定版本详情
 */
export function getArticleVersionDetail(articleId: number, version: number): Promise<ApiResponse<any>> {
  return httpClient.get(`/articles/${articleId}/versions/${version}`)
}

/**
 * 回滚文章版本
 */
export function rollbackArticleVersion(articleId: number, version: number): Promise<ApiResponse<any>> {
  return httpClient.post(`/articles/${articleId}/versions/${version}/rollback`)
}

/**
 * 获取文章的修改建议列表
 */
export function getArticleSuggestions(articleId: number, params?: { status?: number }): Promise<ApiResponse<any[]>> {
  return httpClient.get(`/articles/${articleId}/suggestions`, { params })
}

/**
 * 获取建议详情
 */
export function getSuggestionDetail(articleId: number, suggestionId: number): Promise<ApiResponse<any>> {
  return httpClient.get(`/articles/${articleId}/suggestions/${suggestionId}`)
}

/**
 * 审核修改建议
 */
export function auditSuggestion(articleId: number, suggestionId: number, data: { approved: boolean; reason?: string }): Promise<ApiResponse<any>> {
  return httpClient.put(`/articles/${articleId}/suggestions/${suggestionId}`, data)
}

/**
 * 获取文章贡献者列表
 */
export function getArticleContributors(articleId: number): Promise<ApiResponse<any[]>> {
  return httpClient.get(`/articles/${articleId}/contributors`)
}

/**
 * 获取我收到的建议
 */
export function getReceivedSuggestions(params?: PageParams): Promise<ApiResponse<PageResponse<any>>> {
  return httpClient.get('/articles/suggestions/received-by-me', { params })
}

/**
 * 设置文章推荐状态
 * @param articleId 文章ID
 * @param isFeatured 是否推荐
 * @param featuredLevel 推荐等级（0-普通，1-推荐，2-热门）
 */
export function setArticleFeatured(
  articleId: number,
  isFeatured: boolean,
  featuredLevel?: number
): Promise<ApiResponse<any>> {
  const params = new URLSearchParams()
  params.append('isFeatured', String(isFeatured))
  if (featuredLevel !== undefined) {
    params.append('featuredLevel', String(featuredLevel))
  }
  
  return httpClient.put(`/articles/admin/${articleId}/featured`, params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 设置文章置顶状态
 * @param articleId 文章ID
 * @param isTop 是否置顶
 * @param topLevel 置顶等级（0-不置顶，1-普通置顶，2-重要置顶）
 */
export function setArticleTop(
  articleId: number,
  isTop: boolean,
  topLevel?: number
): Promise<ApiResponse<any>> {
  const params = new URLSearchParams()
  params.append('isTop', String(isTop))
  if (topLevel !== undefined) {
    params.append('topLevel', String(topLevel))
  }
  
  return httpClient.put(`/articles/admin/${articleId}/top`, params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 获取管理后台统计数据
 */
export function getDashboardStats(): Promise<ApiResponse<any>> {
  return httpClient.get('/articles/admin/dashboard-stats')
}

/**
 * 获取文章审核历史
 */
export function getAuditHistory(articleId: number): Promise<ApiResponse<any[]>> {
  return httpClient.get(`/articles/admin/${articleId}/audit-history`)
}

export default {
  getArticleList,
  getArticleById,
  createArticle,
  updateArticle,
  deleteArticle,
  batchAuditArticles,
  batchUpdateCategory,
  batchDeleteArticles,
  getArticleCount,
  getStatsByColumn,
  getStatsByTime,
  getStatsByValue,
  getStatsMultiple,
  searchArticles,
  getArticleVersions,
  getArticleVersionDetail,
  rollbackArticleVersion,
  getArticleSuggestions,
  getSuggestionDetail,
  auditSuggestion,
  getArticleContributors,
  getReceivedSuggestions,
  setArticleFeatured,
  setArticleTop,
  getDashboardStats,
  getAuditHistory
}
