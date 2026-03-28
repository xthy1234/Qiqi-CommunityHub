import http from '@/utils/http'

export interface Draft {
  id?: number | string
  articleId?: number | string | null
  title: string
  content: object
  categoryId?: number | string
  coverUrl?: string
  attachment?: string | null
  latestVersion?: number
  majorVersion?: number
  minorVersion?: number
  versionDisplay?: string
  autoSavedAt?: string
  createdAt?: string
  createTime?: string
  userId?: number | string
  author?: {
    id: number | string
    nickname: string
    avatar: string
  }
  extra?: object
  [key: string]: any
}

export interface DraftResponse {
  draft: Draft
  content: object
  title: string
  extra?: object
  latestVersion?: number
  majorVersion?: number
  minorVersion?: number
  versionDisplay?: string
}

export interface CreateDraftRequest {
  articleId?: number | string
  title?: string
  content?: object
}

export interface CreateDraftResponse {
  draftId: number | string
  articleId?: number | string | null
  hasDraft: boolean
}

export interface AutoSaveDraftRequest {
  title?: string
  content?: object
  coverUrl?: string
  categoryId?: number | string
}

export interface SaveDraftRequest {
  title?: string
  content?: object
  coverUrl?: string
  categoryId?: number | string
  changeSummary?: string
}

export interface SaveDraftResponse {
  version: number
  majorVersion: number
  minorVersion: number
  versionDisplay: string
  title?: string
  content?: object
  coverUrl?: string
  categoryId?: number | string
}

export interface PublishDraftRequest {
  versionType?: number // 0=小版本，1=大版本
  changeSummary?: string
  coverUrl?: string
  categoryId?: number | string
}

export interface PublishDraftResponse {
  articleId: number | string
  version: number
  majorVersion: number
  minorVersion: number
  versionDisplay: string
  versionType?: number
}

/**
 * 文章草稿 API 类
 * 基础路径：/api/articles/drafts
 */
export class DraftAPI {
  private endpoint = '/articles/drafts'
  
  /**
   * 创建新草稿
   * POST /api/articles/drafts
   * @param data 请求参数 { articleId?, title?, content? }
   * @returns draftId, articleId, hasDraft
   */
  createDraft(data: CreateDraftRequest) {
    return http.post<CreateDraftResponse>(this.endpoint, data)
  }
  
  /**
   * 获取草稿详情
   * GET /api/articles/drafts/{draftId}
   * @param draftId 草稿 ID
   */
  getDraft(draftId: number | string) {
    return http.get<DraftResponse>(`${this.endpoint}/${draftId}`)
  }
  
  /**
   * 自动保存草稿
   * PUT /api/articles/drafts/{draftId}
   * @param draftId 草稿 ID
   * @param data 草稿内容 { title, content, coverUrl?, categoryId? }
   */
  autoSaveDraft(draftId: number | string, data: AutoSaveDraftRequest) {
    return http.put(`${this.endpoint}/${draftId}`, data)
  }
  
  /**
   * 手动保存（创建小版本）
   * POST /api/articles/drafts/{draftId}/save
   * @param draftId 草稿 ID
   * @param data 请求参数 { title?, content?, coverUrl?, categoryId?, changeSummary? }
   */
  saveDraft(draftId: number | string, data?: SaveDraftRequest) {
    return http.post<SaveDraftResponse>(`${this.endpoint}/${draftId}/save`, data)
  }
  
  /**
   * 发布文章
   * POST /api/articles/drafts/{draftId}/publish
   * @param draftId 草稿 ID
   * @param data 请求参数 { versionType?, changeSummary? }
   */
  publishDraft(draftId: number | string, data: PublishDraftRequest) {
    return http.post<PublishDraftResponse>(`${this.endpoint}/${draftId}/publish`, data)
  }
  
  /**
   * 删除草稿
   * DELETE /api/articles/drafts/{draftId}
   * @param draftId 草稿 ID
   */
  deleteDraft(draftId: number | string) {
    return http.delete(`${this.endpoint}/${draftId}`)
  }
  
  /**
   * 获取我的草稿列表
   * GET /api/articles/drafts/my?page=1&limit=10
   * @param params 分页参数 { page?, limit? }
   */
  getMyDrafts(params?: { page?: number, limit?: number }) {
    return http.get(`${this.endpoint}/my`, { params })
  }
}

// 导出单例
export const draftAPI = new DraftAPI()
