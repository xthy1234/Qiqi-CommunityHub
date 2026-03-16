/**
 * 文章类型定义
 */
export interface Article {
  id?: number | string
  title: string
  coverUrl: string
  categoryId: number | string
  categoryName: string
  authorId: number | string
  authorNickname: string
  content: string
  attachment?: string | null
  favoriteCount: number
  likeCount: number
  dislikeCount: number
  viewCount: number
  auditStatus: string
  auditReply?: string
  publishTime?: string | null
  deleted?: boolean
  createTime?: string
  updateTime?: string
  categoryStrName?: string | null
  authorAvatar?: string | null
  
  // 兼容旧字段名
  cover?: string
  datetime?: string
  account?: string
  username?: string
  like?: number
  dislike?: number
  favorite?: number
  click?: number
  is_checked?: string
  reply?: string
  attach?: string
  
  [key: string]: any
}

/**
 * 文章查询参数
 */
export interface ArticleQueryParams {
  page?: number
  limit?: number
  title?: string
  categoryId?: number | string
  authorId?: number | string
  auditStatus?: string
  sort?: string
  order?: 'asc' | 'desc'
}

/**
 * API 响应类型
 */
export interface ApiResponse<T = any> {
  code: number
  data: T
  message?: string
}

/**
 * 分页响应类型
 */
export interface PageResponse<T> {
  totalCount: number
  pageSize: number
  totalPage: number
  currPage: number
  list: T[]
}
