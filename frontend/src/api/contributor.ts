import { BaseAPI } from './base'
import http from '@/utils/http'
/**
 * 文章贡献者信息
 */
export interface Contributor {
  userId: number
  nickname: string
  avatar: string
  addedLines: number
  modifiedLines: number
  deletedLines: number
  score: number
  lastContributedAt: string
}

/**
 * 文章贡献者 API
 */
export class ArticleContributorAPI extends BaseAPI {
  /**
   * 获取文章贡献者列表
   */
  getList(articleId: number | string, params?: { page?: number; limit?: number; orderBy?: 'score' | 'lastContributedAt' }) {
    return http.get(`/articles/${articleId}${this.endpoint}`, { params })
  }

  /**
   * 获取贡献者数量
   */
  getCount(articleId: number | string) {
    return http.get(`/articles/${articleId}${this.endpoint}/count`)
  }
}

export const articleContributorAPI = new ArticleContributorAPI('/contributors')
