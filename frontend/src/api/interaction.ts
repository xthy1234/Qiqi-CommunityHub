// src/api/interaction.ts
import http from '@/utils/http'

/**
 * 通用互动查询 API 服务类
 */
class InteractionQueryAPI {
  private endpoint = '/interactions'

  /**
   * 检查操作状态
   * @param contentId 内容ID
   * @param actionType 操作类型：1=收藏，2=点赞，3=点踩，4=分享
   * @param tableName 表名：article 或 comment
   */
  checkStatus(contentId: number | string, actionType: number, tableName: string = 'article') {
    return http.get(`${this.endpoint}/check`, {
      params: { contentId, actionType, tableName }
    })
  }

  /**
   * 获取用户互动历史
   */
  getUserHistory(userId: number | string, actionType?: number, page: number = 1, limit: number = 10) {
    const params: any = { page, limit }
    if (actionType !== undefined) {
      params.actionType = actionType
    }
    return http.get(`${this.endpoint}/user/${userId}`, { params })
  }

  /**
   * 统计用户互动数量
   */
  getUserCount(userId: number | string, actionType?: number) {
    const params: any = {}
    if (actionType !== undefined) {
      params.actionType = actionType
    }
    return http.get(`${this.endpoint}/user/${userId}/count`, { params })
  }
}

// 导出单例
export const interactionQueryAPI = new InteractionQueryAPI()
