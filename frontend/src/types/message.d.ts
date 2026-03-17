
/**
 * 消息实体类型
 */
export interface Message {
  id: number
  fromUserId: number
  toUserId: number
  content: string
  msgType: number // 0-文本，1-图片，2-文件
  status: number // 0-未读，1-已读
  createTime: string
  updateTime?: string
  isSelf?: boolean // 🔥 新增：是否是自己发送的消息
}

/**
 * 发送消息请求 DTO
 */
export interface MessageSendDTO {
  toUserId: number
  content: string
  msgType: number
}

/**
 * 标记已读请求 DTO
 */
export interface MessageReadDTO {
  fromUserId: number
}

/**
 * 会话列表项 VO
 */
export interface ConversationVO {
  userId: number
  username: string
  avatar: string
  lastMessage: string
  lastTime: string
  unreadCount: number
}

/**
 * 发送消息响应 VO
 */
export interface MessageSendResponseVO {
  messageId: number
  createTime: string
}

/**
 * 消息中的用户信息
 */
export interface MessageUser {
  id: number
  username: string
  avatar: string
}

/**
 * 扩展消息类型，包含用户信息
 */
export interface MessageWithUser extends Message {
  fromUser?: MessageUser
  toUser?: MessageUser
}
