// src/api/interaction.ts
import http from '@/utils/http'

export interface InteractionParams {
  contentId: number | string
  actionType: number  // 1=收藏，2=点赞，3=点踩，4=分享
  tableName?: string  // "article" 或 "comment"
  recommendType?: string
  remark?: string
}

/**
 * 互动 API 服务类
 */
class InteractionAPI {
  private endpoint = '/interactions'

  /**
   * 点赞/点踩
   */
  like(params: Omit<InteractionParams, 'actionType'> & { actionType: 2 | 3 }) {
    return http.post(`${this.endpoint}/like`, params)
  }

  /**
   * 取消点赞/点踩
   */
  cancelLike(params: Omit<InteractionParams, 'actionType'> & { actionType: 2 | 3 }) {
    return http.delete(`${this.endpoint}/like`, { data: params })
  }

  /**
   * 收藏/分享
   */
  create(params: InteractionParams) {
    return http.post(this.endpoint, params)
  }

  /**
   * 取消收藏/分享
   */
  cancel(params: Omit<InteractionParams, 'actionType'> & { actionType: 1 | 4 }) {
    return http.delete(`${this.endpoint}/action`, { data: params })
  }

  /**
   * 检查操作状态
   */
  check(contentId: number | string, actionType: number) {
    return http.get(`${this.endpoint}/check`, {
      params: { contentId, actionType }
    })
  }

  /**
   * 获取用户互动记录
   */
  getUserActions(userId: number | string, actionType?: number, page = 1, limit = 10) {
    const params: any = { page, limit }
    if (actionType !== undefined) {
      params.actionType = actionType
    }
    return http.get(`${this.endpoint}/user/${userId}`, { params })
  }
}

// 导出单例
export const interactionAPI = new InteractionAPI()
