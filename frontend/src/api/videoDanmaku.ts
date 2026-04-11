import http from '@/utils/http'

/**
 * 视频弹幕 API
 */
export const videoDanmakuAPI = {
  /**
   * 发送弹幕
   */
  send(data: {
    articleId: number
    videoUrl: string
    content: string
    time: number
    color?: string
    position?: number
    fontSize?: number
  }) {
    return http.post('/api/danmaku', data)
  },

  /**
   * 获取视频弹幕（按时间范围）
   */
  getList(articleId: number, videoUrl: string, from?: number, to?: number) {
    return http.get('/api/danmaku', {
      params: { articleId, videoUrl, from, to }
    })
  },

  /**
   * 获取最新弹幕
   */
  getLatest(videoUrl: string, limit: number = 100, articleId?: number) {
    const params: any = { videoUrl, limit }
    if (articleId) {
      params.articleId = articleId
    }
    return http.get('/api/danmaku/latest', { params })
  },

  /**
   * 屏蔽弹幕（管理员）
   */
  block(danmakuId: number) {
    return http.put(`/api/danmaku/${danmakuId}/block`)
  },

  /**
   * 分页查询弹幕
   */
  getPage(params: {
    page: number
    limit: number
    articleId?: number
    videoUrl?: string
    userId?: number
    status?: number
    keyword?: string
  }) {
    return http.get('/api/danmaku/page', { params })
  },

  /**
   * 统计视频弹幕数量
   */
  getCount(articleId: number, videoUrl?: string) {
    const params: any = { articleId }
    if (videoUrl) {
      params.videoUrl = videoUrl
    }
    return http.get('/api/danmaku/count', { params })
  }
}
