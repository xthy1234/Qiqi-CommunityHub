// src/types/circleChat.d.ts

import type { MessageStatus } from './message'

/** 圈子角色类型 */
export type CircleRole = 'OWNER' | 'ADMIN' | 'MEMBER'

/** 圈子成员接口 */
export interface CircleMember {
  id: number
  userId: number
  circleId: number
  nickname: string
  avatar: string
  role: CircleRole
  joinTime: string
  isOnline?: boolean
}

/** 圈子信息接口 */
export interface Circle {
  id: number
  name: string
  avatar?: string
  description?: string
  ownerId: number
  ownerNickname?: string
  memberCount: number
  myRole?: CircleRole
  createTime: string
  notice?: string
  type?: number  // 0-私密，1-公开
}

/** 圈子会话接口 */
export interface CircleConversation {
  circleId: number
  circleName: string
  circleAvatar?: string
  lastMessageContent?: string
  lastMessageSenderId?: number
  lastMessageSenderNickname?: string
  lastMessageTime: string
  unreadCount: number
  memberCount?: number
  myRole?: CircleRole
}

/** 圈子消息接口 */
export interface CircleMessage {
  id: number
  circleId: number
  senderId: number
  sender?: {
    id: number
    nickname: string
    avatar: string
  }
  content: string
  msgType: number  // 0-文本，1-图片，2-文件
  status?: MessageStatus
  createTime: string
  isSelf?: boolean
  isRecalled?: boolean
  deletedByAdmin?: boolean  // 修改：是否被管理员删除
  deleter?: {
    id: number
    nickname: string
    avatar: string
  }
  deletedTime?: string
  action?: 'SEND' | 'RECALL' | 'DELETE'  // 新增 DELETE 动作
  _isSystemTip?: boolean
  _tipType?: 'join' | 'quit' | 'kick' | 'recall' | 'delete'  // 新增 delete 类型
  _tipUsername?: string
  _deleteDetail?: string  // 新增：删除详情描述
  _sending?: boolean
  _tempId?: string
}

/** 分页参数 */
export interface PaginationParams {
  page: number
  limit: number
}

/** 分页结果 */
export interface PaginationResult<T> {
  list: T[]
  total: number
  page: number
  limit: number
}

/** 邀请链接响应 */
export interface InviteLinkResponse {
  inviteLink: string
  inviteCode: string
  expireTime: string
}
