/**
 * 私信消息实体
 */
export interface Message {
  id: number
  conversationId: number
  fromUserId: number
  toUserId: number
  content: string
  contentType: 'TEXT' | 'IMAGE' | 'FILE'
  status: 'SENT' | 'DELIVERED' | 'READ' | 'FAILED'
  createdAt: string
  updatedAt: string
}

/**
 * 发送消息 DTO
 */
export interface MessageSendDTO {
  toUserId: number
  content: string
  contentType?: 'TEXT' | 'IMAGE' | 'FILE'
}

/**
 * 发送消息响应 VO
 */
export interface MessageSendResponseVO {
  message: Message
  shouldCreateConversation: boolean
  conversation?: ConversationVO
}

/**
 * 会话 VO
 */
export interface ConversationVO {
  id: number
  participantIds: number[]
  lastMessage: Message
  unreadCount: number
  updatedAt: string
  // 扩展字段：用于前端展示
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
