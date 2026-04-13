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
 * 获取文章列表
 */
export function getArticleList(params: PageParams): Promise<ApiResponse<PageResponse<any>>> {
  return httpClient.get('/articles', { params })
}

/**
 * 获取文章详情
 */
export function getArticleById(id: number): Promise<ApiResponse<any>> {
  return httpClient.get(`/articles/${id}`)
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
  return httpClient.post('/articles/batch-audit', data)
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

export default {
  getArticleList,
  getArticleById,
  createArticle,
  updateArticle,
  deleteArticle,
  batchAuditArticles,
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
  getReceivedSuggestions
}
