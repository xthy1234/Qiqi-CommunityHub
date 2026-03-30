import http from '@/utils/http'

export interface ReportCreateDTO {
  contentId: number              // 被举报内容 ID
  contentTitle?: string          // 可选：标题
  contentCategory?: string       // 可选：分类
  reportedUserID?: number        // 可选：被举报用户 ID
  reportedUserAccount?: string   // 可选：被举报账号
  reportedNickName?: string      // 可选：被举报昵称
  reportReason: string           // 必填：举报原因
}

class ReportAPI {
  private endpoint = '/reports'

  /**
   * 创建举报
   */
  createReport(data: ReportCreateDTO) {
    return http.post(this.endpoint, data)
  }
}

export const reportAPI = new ReportAPI()
