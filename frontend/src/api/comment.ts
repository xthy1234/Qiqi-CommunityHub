// src/api/comment.ts
import http from '@/utils/http'

export interface Comment {
  id?: number
  contentId: number | string
  userId?: number | string
  userNickname: string
  userAvatar?: string
  content: string
  parentId?: number
  likeCount?: number
  createTime?: string
  children?: Comment[]
  isLiked?: boolean
  isDisliked?: boolean
}

export interface CreateCommentParams {
  contentId: number | string
  content: string
  parentId?: number
}

export interface ReplyCommentParams {
  contentId: number | string
  parentId: number | string
  replyContent: string
}

/**
 * 评论 API 服务类
 */
class CommentAPI {
  private endpoint = '/comments'

  /**
   * 创建评论
   */
  create(data: CreateCommentParams) {
    return http.post(this.endpoint, data)
  }

  /**
   * 回复评论
   */
  reply(data: ReplyCommentParams) {
    return http.post(`${this.endpoint}/reply`, data)
  }

  /**
   * 获取文章评论列表（扁平结构）
   */
  getByContentId(contentId: number | string) {
    return http.get(`${this.endpoint}/content/${contentId}`)
  }

  /**
   * 获取文章评论树形结构
   */
  getTree(contentId: number | string) {
    return http.get(`${this.endpoint}/tree/${contentId}`)
  }

  /**
   * 获取评论数量
   */
  getCount(contentId: number | string) {
    return http.get(`${this.endpoint}/count/${contentId}`)
  }

  // /**
  //  * 删除评论
  //  */
  // delete(id: number | string) {
  //   return http.delete(`${this.endpoint}/${id}`)
  // }
  //
  // /**
  //  * 批量删除评论
  //  */
  // batchDelete(ids: number[]) {
  //   return http.post(`${this.endpoint}/batch-delete`, { ids })
  // }

  /**
   * 点赞评论
   */
  like(commentId: number | string) {
    return http.post(`${this.endpoint}/${commentId}/like`)
  }

  /**
   * 删除评论
   */
  delete(commentId: number | string) {
    return http.delete(`${this.endpoint}/${commentId}`)
  }
}

// 导出单例
export const commentAPI = new CommentAPI()
