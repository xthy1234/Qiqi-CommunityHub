/**
 * 私信消息实体
 */
export interface Message {
  id: number
  conversationId?: number
  fromUserId: number
  toUserId: number
  content: string
  //  兼容两种类型：新的 contentType 和旧的 msgType
  contentType?: 'TEXT' | 'IMAGE' | 'FILE'
  msgType?: number // 0-文本，1-图片，2-文件
  //  兼容两种状态类型：新的字符串枚举和旧的数字
  status?: 'SENT' | 'DELIVERED' | 'READ' | 'FAILED' | 'SENDING' | number
  createTime: string
  updateTime?: string
  createdAt?: string
  updatedAt?: string
  isSelf?: boolean // 是否是自己发送的消息
  //  新增：临时 ID（用于乐观更新）
  _tempId?: string
  //  新增：发送中状态
  _sending?: boolean
  //  新增：后端返回的撤回状态
  isRecalled?: boolean
  deletedBySender?: boolean
  deletedByRecipient?: boolean
  //  新增：系统提示相关字段（前端扩展）
  _isSystemTip?: boolean
  _tipType?: 'recall' | 'delete' | 'session-start' | 'custom'
  _tipUsername?: string
  //  扩展用户信息
  fromUser?: MessageUser
  toUser?: MessageUser
}

/**
 * 发送消息 DTO
 */
export interface MessageSendDTO {
  toUserId: number
  content: string
  contentType?: 'TEXT' | 'IMAGE' | 'FILE'
  msgType?: number
}

/**
 * 标记已读请求 DTO
 */
export interface MessageReadDTO {
  fromUserId: number
}

/**
 * 发送消息响应 VO
 */
export interface MessageSendResponseVO {
  message?: Message
  messageId?: number
  createTime?: string
  shouldCreateConversation?: boolean
  conversation?: ConversationVO
}

/**
 * 会话 VO - 兼容两种定义
 */
export interface ConversationVO {
  // 新版本的字段（完整的会话信息）
  id?: number
  participantIds?: number[]
  lastMessage?: Message | string //  兼容字符串和完整消息对象
  unreadCount?: number
  updatedAt?: string
  
  // 旧版本的字段（简化的会话信息）
  userId?: number
  username?: string
  avatar?: string
  lastTime?: string
}

/**
 * WebSocket 消息类型定义
 */
export interface WsChatMessage {
  type: 'CHAT_MESSAGE'
  data: {
    messageId: number
    conversationId: number
    fromUserId: number
    toUserId: number
    content: string
    contentType: 'TEXT' | 'IMAGE' | 'FILE'
    timestamp: number
  }
}

export interface WsMessageStatusUpdate {
  type: 'MESSAGE_STATUS'
  data: {
    messageId: number
    status: 'SENT' | 'DELIVERED' | 'READ'
    timestamp: number
  }
}

export interface WsNewConversation {
  type: 'NEW_CONVERSATION'
  data: {
    conversationId: number
    participantIds: number[]
    createdAt: number
  }
}

export interface WsUnreadCountUpdate {
  type: 'UNREAD_COUNT_UPDATE'
  data: {
    conversationId: number
    unreadCount: number
    totalCount: number
  }
}

/**
 * 所有 WebSocket 消息类型的联合类型
 */
export type WsChatMessageType = 
  | WsChatMessage
  | WsMessageStatusUpdate
  | WsNewConversation
  | WsUnreadCountUpdate

/**
 * 消息处理器类型
 */
export type MessageHandler = (data: any) => void

/**
 * WebSocket 连接状态
 */
export enum WsConnectionState {
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  DISCONNECTED = 'disconnected',
  RECONNECTING = 'reconnecting'
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

/**
 * 消息状态常量（字符串枚举）
 */
export const MessageStatus = {
    /** 已发送（对方未读）*/
    SENT: 'SENT' as const,
    /** 已读 */
    READ: 'READ' as const,
    /** 已送达 */
    DELIVERED: 'DELIVERED' as const,
    /** 发送失败 */
    FAILED: 'FAILED' as const,
    /** 发送中 */
    SENDING: 'SENDING' as const
} as const

export type MessageStatusType = typeof MessageStatus[keyof typeof MessageStatus]

/**
 * 判断消息是否为未读状态
 * 
 * 业务规则：
 * - SENT (0): 消息已发送，对方未读
 * - DELIVERED (2): 消息已送达对方设备，但可能还未读
 * 
 * @param status 消息状态值（字符串或数字）
 * @returns 是否为未读状态
 */
export function isUnreadMessage(status?: string | number): boolean {
    //  关键修复：同时检查数字和字符串类型
    return status === MessageStatus.SENT ||        // 'SENT' 字符串
           status === MessageStatus.DELIVERED ||   // 'DELIVERED' 字符串
           status === 0 ||                         // 数字 0 (SENT)
           status === 2                            // 数字 2 (DELIVERED)
}

/**
 * 判断消息是否为已读状态
 * 
 * 业务规则：
 * - READ (1): 消息已被对方阅读
 * 
 * @param status 消息状态值（字符串或数字）
 * @returns 是否为已读状态
 */
export function isReadMessage(status?: string | number): boolean {
    //  关键修复：同时检查数字和字符串类型
    return status === MessageStatus.READ ||    // 'READ' 字符串
           status === 1                        // 数字 1 (READ)
}

/**
 * 判断消息是否正在发送
 * 
 * @param status 消息状态值（字符串或数字）
 * @returns 是否正在发送
 */
export function isSendingMessage(status?: string | number): boolean {
    return status === MessageStatus.SENDING || status === 'SENDING'
}

/**
 * 判断消息是否发送失败
 * 
 * @param status 消息状态值（字符串或数字）
 * @returns 是否发送失败
 */
export function isFailedMessage(status?: string | number): boolean {
    return status === MessageStatus.FAILED || status === 'FAILED' || status === 3
}
