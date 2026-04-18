// src/api/comment.ts
import http from '@/utils/http'

export interface UserSimpleInfo {
  id: number | string
  nickname: string
  avatar?: string
  lastOnlineTime?: string
}

export interface Comment {
  id?: number
  contentId?: number | string
  userId?: number | string
  userNickname?: string
  userAvatar?: string
  user?: UserSimpleInfo
  content: string
  parentId?: number | null
  replyId?: number | string | null
  replyToUser?: UserSimpleInfo | null
  likeCount?: number
  dislikeCount?: number
  createTime?: string
  updateTime?: string | null
  isEdited?: boolean
  replyCount?: number
  children?: Comment[]
  topReplies?: Comment[]
  isLiked?: boolean
  isDisliked?: boolean
  status?: number
  level?: number
}

export interface CreateCommentParams {
  contentId: number | string
  content: string
  parentId?: number | null
}

export interface ReplyCommentParams {
  contentId: number | string
  replyContent: string
  replyId?: number | string        // 可选：被回复的用户ID
}

export interface UpdateCommentParams {
  id: number | string
  content: string
  status?: number
}

export interface CommentListResponse {
  totalRecord: number
  totalPage: number
  currPage: number
  pageSize: number
  list: Comment[]
}

/**
 * 评论 API 服务类
 */
class CommentAPI {
  private commentEndpoint = '/comments'
  private articleEndpoint = '/articles'

  /**
   * 获取文章评论列表（分页 + 预加载前3条子评论）
   * @param articleId 文章ID
   * @param type 评论类型：'list'(默认) 或 'tree'
   * @param page 页码，默认 1
   * @param size 每页数量，默认 10
   */
  getArticleComments(articleId: number | string, type: 'list' | 'tree' = 'list', page: number = 1, size: number = 10) {
    return http.get(`${this.articleEndpoint}/${articleId}/comments`, {
      params: { type, page, size }
    })
  }

  /**
   * 统计文章评论数
   * @param articleId 文章ID
   */
  getArticleCommentCount(articleId: number | string) {
    return http.get(`${this.articleEndpoint}/${articleId}/comments/count`)
  }

  /**
   * 获取评论详情
   * @param id 评论ID
   */
  getCommentDetail(id: number | string) {
    return http.get(`${this.commentEndpoint}/${id}`)
  }

  /**
   * 创建主评论
   * @param data 评论数据
   */
  createComment(data: CreateCommentParams) {
    return http.post(this.commentEndpoint, {
      contentId: data.contentId,
      content: data.content,
      parentId: data.parentId || null
    })
  }

  /**
   * 发表回复（子评论）
   * @param targetCommentId 被回复的评论ID（路径参数，后端从此处获取 parentId）
   * @param data 回复数据
   */
  replyComment(targetCommentId: number | string, data: ReplyCommentParams) {

    
    return http.post(`${this.commentEndpoint}/${targetCommentId}/replies`, {
      contentId: data.contentId,
      replyContent: data.replyContent,
      replyId: data.replyId || null
    })
  }

  /**
   * 获取子评论列表（分页）
   * @param parentCommentId 父评论ID（主评论ID）
   * @param page 页码
   * @param size 每页数量
   */
  getReplies(parentCommentId: number | string, page: number = 1, size: number = 10) {
    return http.get(`${this.commentEndpoint}/${parentCommentId}/replies`, {
      params: { page, size }
    })
  }

  /**
   * 点赞评论（切换模式：已赞则取消，未赞则点赞）
   */
  toggleLike(commentId: number | string) {
    return http.post(`${this.commentEndpoint}/${commentId}/likes`)
  }

  /**
   * 点踩评论（切换模式：已踩则取消，未踩则点踩）
   */
  toggleDislike(commentId: number | string) {
    return http.post(`${this.commentEndpoint}/${commentId}/dislikes`)
  }

  /**
   * 更新评论
   * @param id 评论ID
   * @param data 更新数据
   */
  patchComment(id: number | string, data: UpdateCommentParams) {
    return http.put(`${this.commentEndpoint}/${id}`, {
      content: data.content,
      status: data.status
    })
  }

  /**
   * 删除评论
   * @param id 评论ID
   */
  deleteComment(id: number | string) {
    return http.delete(`${this.commentEndpoint}/${id}`)
  }
}

// 导出单例
export const commentAPI = new CommentAPI()
