import http from '@/utils/http'

/**
 * 用户信息接口
 */
export interface UserInfo {
  id: number
  nickname: string
  avatar?: string
  lastOnlineTime?: string
}

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
  operator?: UserInfo
  contributorId?: number
  contributor?: UserInfo
  createTime: string
  content?: object
  majorVersion: number
  minorVersion: number
  isLatest?: boolean
  isCurrent?: boolean
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
   * @returns List<ArticleVersion> 版本列表，按 createTime 倒序
   */
  getList(articleId: number | string, params?: any) {
    return http.get(`/articles/${articleId}${this.endpoint}`, { params })
  }
  
  /**
   * 获取版本详情
   * @param articleId 文章 ID
   * @param version 版本号
   * @returns ArticleVersion 包含完整的 content 内容
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
   * 注意：回滚不会直接修改版本号，而是创建一个新的版本记录
   * 新版本的内容 = 目标版本的内容
   * 新版本的版本号 = 自动递增（max(version) + 1）
   * 文章的 currentVersion 会更新为新版本的 version
   * @param articleId 文章 ID
   * @param version 要回滚到的版本号
   * @param data 回滚参数（包含 version 字段）
   * @returns 成功返回空或新版本信息
   */
  rollback(articleId: number | string, version: number, data: RollbackParams) {
    return http.post(`/articles/${articleId}${this.endpoint}/${version}/rollback`, data)
  }
}

// 导出单例
export const articleVersionAPI = new ArticleVersionAPI()
