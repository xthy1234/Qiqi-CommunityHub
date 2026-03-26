//src/api/articleSuggestion.ts
import http from '@/utils/http'

/**
 * 修改建议接口
 */
export interface ArticleEditSuggestion {
  id: number
  articleId: number
  articleTitle?: string  // 文章标题（用于列表展示）
  proposerId: number
  proposerName?: string
  proposerAvatar?: string
  title: string
  content: object  // TipTap JSON 格式
  changeSummary?: string
  status: 0 | 1 | 2  // 0-待审核，1-已通过，2-已拒绝
  createTime: string
  reviewTime?: string
  reviewerId?: number
  reviewerName?: string
  rejectReason?: string  // 拒绝理由
}

/**
 * 建议审核请求参数
 */
export interface ReviewSuggestionParams {
  approved: boolean  // true-通过，false-拒绝
  reason?: string  // 拒绝理由
}

/**
 * 文章编辑建议 API 类
 */
export class ArticleSuggestionAPI {
  private endpoint = '/articles/suggestions'
  
  /**
   * 获取当前用户收到的修改建议列表（作为作者）
   * @param params 查询参数（status, page, limit）
   */
  getMyReceivedSuggestions(params?: any) {
    return http.get(`${this.endpoint}/received-by-me`, { params })
  }
  
  /**
   * 获取当前用户提交的修改建议列表（作为提议者）
   * @param params 查询参数（status, page, limit）
   */
  getMySuggestions(params?: any) {
    return http.get(`${this.endpoint}/proposed-by-me`, { params })
  }
  
  /**
   * 获取文章的修改建议列表
   * @param articleId 文章 ID
   * @param params 查询参数（status, page, limit）
   */
  getList(articleId: number | string, params?: any) {
    return http.get(`${this.endpoint}/article/${articleId}`, { params })
  }
  
  /**
   * 获取建议详情
   * @param suggestionId 建议 ID
   */
  getById(suggestionId: number | string) {
    return http.get(`${this.endpoint}/${suggestionId}`)
  }
  
  /**
   * 提交修改建议
   * @param articleId 文章 ID
   * @param data 建议内容
   */
  create(articleId: number | string, data: Partial<ArticleEditSuggestion>) {
    return http.post(`${this.endpoint}/article/${articleId}`, data)
  }
  
  /**
   * 审核建议（通过/拒绝）
   * @param suggestionId 建议 ID
   * @param data 审核结果
   */
  review(suggestionId: number | string, data: ReviewSuggestionParams) {
    return http.put(`${this.endpoint}/${suggestionId}`, data)
  }
  
  /**
   * 删除建议
   * @param suggestionId 建议 ID
   */
  delete(suggestionId: number | string) {
    return http.delete(`${this.endpoint}/${suggestionId}`)
  }
}

// 导出单例
export const articleSuggestionAPI = new ArticleSuggestionAPI()
