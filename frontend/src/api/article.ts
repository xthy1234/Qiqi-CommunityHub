import http from '@/utils/http'

export interface Article {
  id: number | string
  title: string
  coverUrl: string
  categoryId: number | string
  categoryName: string
  authorId: number | string
  authorNickname: string
  content: object
  attachment?: string | null
  favoriteCount: number
  likeCount: number
  dislikeCount: number
  viewCount: number
  commentCount?: number
  auditStatus: string | 'PENDING' | 'APPROVED' | 'REJECTED' | 'DRAFT'
  auditReply?: string
  publishTime?: string | null
  deleted?: boolean
  createTime?: string
  updateTime?: string
  categoryStrName?: string | null
  authorAvatar?: string | null
  isDraft?: boolean
  // 兼容旧字段
  cover?: string
  datetime?: string
  account?: string
  username?: string
  like?: number
  dislike?: number
  favorite?: number
  click?: number
  is_checked?: string
  reply?: string
  attach?: string
  [key: string]: any
}

/**
 * 文章 API 类
 */
export class ArticleAPI {
  private endpoint = '/articles'
  private draftEndpoint = '/articles/draft'
  private publishedEndpoint = '/articles/published'
  
  /**
   * 获取文章列表
   */
  getList(params?: any) {
    return http.get(this.endpoint, { params })
  }
  
  /**
   * 获取文章详情
   */
  getById(id: number | string) {
    return http.get(`${this.endpoint}/${id}`)
  }
  
  /**
   * 创建文章
   */
  create(data: Article) {
    return http.post(this.endpoint, data)
  }
  
  /**
   * 更新文章
   */
  update(id: number | string, data: Partial<Article>) {
    return http.put(`${this.endpoint}/${id}`, data)
  }
  
  /**
   * 删除文章（通用接口，自动判断状态和权限）
   */
  delete(id: number | string) {
    return http.delete(`${this.endpoint}/${id}`)
  }
  
  /**
   * 点赞文章
   */
  like(id: number | string) {
    return http.post(`${this.endpoint}/${id}/like`)
  }

  /**
   * 批量审核文章
   * 接口：POST /articles/batch-audit?status=1&reply=xxx
   * 请求体：[id1, id2, id3]
   */
  batchAudit(data: { ids: number[], status: number | string, reply?: string }) {
    return http.post(`${this.endpoint}/batch-audit`, data.ids, {
      params: {
        status: data.status,
        reply: data.reply
      }
    })
  }
  
  /**
   * 批量删除文章
   * 接口：POST /articles/batch-delete
   * 请求体：[1, 2, 3]
   */
  batchDelete(ids: number[]) {
    return http.post(`${this.endpoint}/batch-delete`, ids)
  }
  
  /**
   * 创建草稿
   */
  createDraft(data: Partial<Article>) {
    return http.post(this.draftEndpoint, data)
  }
  
  /**
   * 更新草稿
   */
  updateDraft(id: number | string, data: Partial<Article>) {
    return http.put(`${this.draftEndpoint}/${id}`, data)
  }
  
  /**
   * 提交草稿审核
   */
  submitDraft(id: number | string) {
    return http.post(`${this.draftEndpoint}/${id}/submit`)
  }
  
  /**
   * 获取我的草稿列表
   * 接口：GET /articles/draft/my
   */
  getMyDrafts(params?: any) {
    return http.get(`${this.endpoint}`, { params })
  }
  
  /**
   * 获取草稿详情
   */
  getDraftById(id: number | string) {
    return http.get(`${this.draftEndpoint}/${id}`)
  }
  
  /**
   * 删除草稿（仅作者本人可操作）
   * 接口：DELETE /articles/draft/{id}
   */
  deleteDraft(id: number | string) {
    return http.delete(`${this.draftEndpoint}/${id}`)
  }
  
  /**
   * 批量删除草稿
   * 接口：POST /articles/draft/batch-delete
   * 请求体：[id1, id2, id3]
   */
  batchDeleteDrafts(ids: number[]) {
    return http.post(`${this.draftEndpoint}/batch-delete`, ids)
  }

  /**
   * 获取文章统计值（单列）
   */
  getStatsValue(xColumn: string, yColumn: string) {
    return http.get(`${this.endpoint}/stats/value/${xColumn}/${yColumn}`)
  }

  /**
   * 获取文章统计值（多列）
   */
  getStatsValueMultiple(xColumn: string) {
    return http.get(`${this.endpoint}/stats/value/multiple/${xColumn}`)
  }

  /**
   * 获取文章统计时间序列
   */
  getStatsTimeSeries(xColumn: string, yColumn: string, timeType: string) {
    return http.get(`${this.endpoint}/stats/time/${xColumn}/${yColumn}/${timeType}`)
  }

  /**
   * 获取文章分组统计
   */
  getStatsGroup(column: string) {
    return http.get(`${this.endpoint}/stats/group/${column}`)
  }

  /**
   * 搜索文章
   */
  searchArticles(params?: any) {
    return http.get(`${this.endpoint}/search`, { params })
  }

  /**
   * 获取文章总数
   */
  getCount(params?: any) {
    return http.get(`${this.endpoint}/count`, { params })
  }

  /**
   * 获取已发布文章详情
   */
  getPublishedById(id: number | string) {
    return http.get(`${this.publishedEndpoint}/${id}`)
  }
}

// 导出单例
export const articleAPI = new ArticleAPI()

// 兼容旧的导出方式（逐步迁移）
export const getArticleList = (params: any) => articleAPI.getList(params)
export const getArticleDetail = (id: number | string) => articleAPI.getById(id)
export const createArticle = (data: Article) => articleAPI.create(data)
export const updateArticle = (id: number | string, data: Partial<Article>) => articleAPI.update(id, data)
export const deleteArticle = (id: number | string) => articleAPI.delete(id)
export const likeArticle = (id: number | string) => articleAPI.like(id)
export const batchAuditArticles = (data: { ids: number[], status: number | string, reply?: string }) => articleAPI.batchAudit(data)
export const batchDeleteArticles = (ids: number[]) => articleAPI.batchDelete(ids)
export const createDraft = (data: Partial<Article>) => articleAPI.createDraft(data)
export const updateDraft = (id: number | string, data: Partial<Article>) => articleAPI.updateDraft(id, data)
export const submitDraft = (id: number | string) => articleAPI.submitDraft(id)
export const getMyDrafts = (params?: any) => articleAPI.getMyDrafts(params)
export const getDraftById = (id: number | string) => articleAPI.getDraftById(id)
export const deleteDraft = (id: number | string) => articleAPI.deleteDraft(id)
export const getPublishedArticleById = (id: number | string) => articleAPI.getPublishedById(id)
export const searchArticles = (params?: any) => articleAPI.searchArticles(params)
export const getArticleCount = (params?: any) => articleAPI.getCount(params)
export const getArticleStatsValue = (xColumn: string, yColumn: string) => articleAPI.getStatsValue(xColumn, yColumn)
export const getArticleStatsValueMultiple = (xColumn: string) => articleAPI.getStatsValueMultiple(xColumn)
export const getArticleStatsTimeSeries = (xColumn: string, yColumn: string, timeType: string) => articleAPI.getStatsTimeSeries(xColumn, yColumn, timeType)
export const getArticleStatsGroup = (column: string) => articleAPI.getStatsGroup(column)
