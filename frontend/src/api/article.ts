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
  summary?: string
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
  isLiked?: boolean
  isDisliked?: boolean
  isFavorited?: boolean
  likeCount?: number
  dislikeCount?: number
  favoriteCount?: number
  [key: string]: unknown
}

/**
 * 文章 API 服务类
 */
class ArticleAPI {
  private endpoint = '/articles'

  /**
   * 获取文章列表
   */
  getList(params?: Record<string, unknown>) {
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
   * 点赞文章（切换模式：已赞则取消，未赞则点赞）
   */
  toggleLike(articleId: number | string) {
    return http.post(`${this.endpoint}/${articleId}/likes`)
  }

  /**
   * 点踩文章（切换模式：已踩则取消，未踩则点踩）
   */
  toggleDislike(articleId: number | string) {
    return http.post(`${this.endpoint}/${articleId}/dislikes`)
  }

  /**
   * 收藏文章（切换模式：已收藏则取消，未收藏则收藏）
   */
  toggleFavorite(articleId: number | string) {
    return http.post(`${this.endpoint}/${articleId}/favorites`)
  }

  /**
   * 获取用户对文章的互动状态
   */
  getInteractionStatus(articleId: number | string) {
    return http.get(`${this.endpoint}/${articleId}/interaction-status`)
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
  searchArticles(params?: Record<string, unknown>) {
    return http.get(`${this.endpoint}/search`, { params })
  }

  /**
   * 获取文章总数
   */
  getCount(params?: Record<string, unknown>) {
    return http.get(`${this.endpoint}/count`, { params })
  }

  /**
   * 获取已发布文章详情
   */
  getPublishedById(id: number | string) {
    return http.get(`${this.endpoint}/${id}`)
  }

  /**
   * 更新文章编辑模式
   * 接口：PUT /articles/{articleId}/edit-mode?editMode=0|1
   */
  updateEditMode(id: number | string, editMode: number) {
    return http.put(`${this.endpoint}/${id}/edit-mode`, null, {
      params: { editMode }
    })
  }

  /**
   * 增加文章浏览量
   * 接口：POST /articles/{id}/view
   */
  incrementViewCount(id: number | string) {
    return http.post(`${this.endpoint}/${id}/view`)
  }
}


export const articleAPI = new ArticleAPI()

// 兼容旧的导出方式（逐步迁移）
export const getArticleList = (params: Record<string, unknown>) => articleAPI.getList(params)
export const getArticleDetail = (id: number | string) => articleAPI.getById(id)
export const createArticle = (data: Article) => articleAPI.create(data)
export const updateArticle = (id: number | string, data: Partial<Article>) => articleAPI.update(id, data)
export const deleteArticle = (id: number | string) => articleAPI.delete(id)
export const likeArticle = (id: number | string) => articleAPI.toggleLike(id)
export const batchAuditArticles = (data: { ids: number[], status: number | string, reply?: string }) => articleAPI.batchAudit(data)
export const batchDeleteArticles = (ids: number[]) => articleAPI.batchDelete(ids)
export const getPublishedArticleById = (id: number | string) => articleAPI.getPublishedById(id)
export const searchArticles = (params?: Record<string, unknown>) => articleAPI.searchArticles(params)
export const getArticleCount = (params?: Record<string, unknown>) => articleAPI.getCount(params)
export const getArticleStatsValue = (xColumn: string, yColumn: string) => articleAPI.getStatsValue(xColumn, yColumn)
export const getArticleStatsValueMultiple = (xColumn: string) => articleAPI.getStatsValueMultiple(xColumn)
export const getArticleStatsTimeSeries = (xColumn: string, yColumn: string, timeType: string) => articleAPI.getStatsTimeSeries(xColumn, yColumn, timeType)
export const getArticleStatsGroup = (column: string) => articleAPI.getStatsGroup(column)
export const incrementArticleViewCount = (id: number | string) => articleAPI.incrementViewCount(id)
