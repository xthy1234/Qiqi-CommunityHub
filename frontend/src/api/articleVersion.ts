import http from '@/utils/http'

/**
 * 文章版本接口
 */
export interface ArticleVersion {
  id: number
  articleId: number
  version: number
  title: string
  changeSummary?: string
  operatorId?: number
  operatorName?: string
  createTime: string
  content?: object  // TipTap JSON 格式
}

/**
 * 版本对比结果
 */
export interface VersionCompareResult {
  sourceVersion: ArticleVersion
  targetVersion: ArticleVersion
}

/**
 * 回滚请求参数
 */
export interface RollbackParams {
  version: number
}

/**
 * 文章版本 API 类
 */
export class ArticleVersionAPI {
  private endpoint = '/versions'
  
  /**
   * 获取文章的版本列表
   * @param articleId 文章 ID
   * @param params 查询参数（page, limit）
   */
  getList(articleId: number | string, params?: any) {
    return http.get(`/articles/${articleId}${this.endpoint}`, { params })
  }
  
  /**
   * 获取版本详情
   * @param articleId 文章 ID
   * @param version 版本号
   */
  getById(articleId: number | string, version: number) {
    return http.get(`/articles/${articleId}${this.endpoint}/${version}`)
  }
  
  /**
   * 对比两个版本
   * @param articleId 文章 ID
   * @param versionA 版本 A
   * @param versionB 版本 B
   */
  compare(articleId: number | string, versionA: number, versionB: number) {
    return http.get(`/articles/${articleId}${this.endpoint}/compare`, {
      params: { versionA, versionB }
    })
  }
  
  /**
   * 回滚到指定版本
   * @param articleId 文章 ID
   * @param version 版本号
   * @param data 回滚参数
   */
  rollback(articleId: number | string, version: number, data: RollbackParams) {
    return http.post(`/articles/${articleId}${this.endpoint}/${version}/rollback`, data)
  }
}

// 导出单例
export const articleVersionAPI = new ArticleVersionAPI()
