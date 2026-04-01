import http from '@/utils/http'

export interface ReportCreateDTO {
  contentId: number              // 被举报内容 ID
  reportReason: string           // 必填：举报原因
  reportType?: 'ARTICLE' | 'COMMENT' | 'USER' | string  // 可选：举报类型，默认 ARTICLE
}

class ReportAPI {
  private endpoint = '/reports'

  /**
   * 创建举报（简化版）
   * @param data 举报数据
   */
  createReport(data: ReportCreateDTO) {
    return http.post(this.endpoint, data)
  }
}

export const reportAPI = new ReportAPI()
