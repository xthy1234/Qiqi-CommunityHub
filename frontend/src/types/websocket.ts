import type { Message } from '@/types/message'
import type { CircleMessage } from '@/types/circleChat'

/**
 * WebSocket 消息类型定义
 */
export interface WsMessagePayload<T extends string = string, D = any> {
  type: T
  data: D
  timestamp?: number
}

/**
 * 私聊消息 payload
 */
export interface PrivateMessagePayload extends WsMessagePayload<'CHAT_MESSAGE', Message> {}

/**
 * 消息状态更新 payload
 */
export interface MessageStatusPayload extends WsMessagePayload<'MESSAGE_STATUS', {
  messageId: number
  status: 'SENT' | 'DELIVERED' | 'READ'
  readAt?: string
}> {}

/**
 * 撤回通知 payload
 */
export interface MessageRecallPayload extends WsMessagePayload<'MESSAGE_RECALL', {
  messageId: number
  userId: number
  reason?: string
  recalledAt: string
}> {}

/**
 * 用户在线状态 payload
 */
export interface UserOnlineStatusPayload extends WsMessagePayload<'USER_ONLINE_STATUS', {
  userId: number
  isOnline: boolean
  lastSeenAt?: string
  timestamp: number
}> {}

/**
 * 圈子消息 payload
 */
export interface CircleMessagePayload extends WsMessagePayload<'CIRCLE_CHAT_MESSAGE', CircleMessage> {}

/**
 * 圈子消息删除通知
 */
export interface CircleMessageDeletePayload extends WsMessagePayload<'CIRCLE_CHAT_MESSAGE_DELETE', {
  messageId: number
  circleId: number
  deleterId: number
  deleterNickname?: string
  deletedAt: string
}> {}

/**
 * 消息删除通知
 */
export interface MessageDeletePayload extends WsMessagePayload<'MESSAGE_DELETE', {
  messageId: number
  userId: number
  deletedAt: string
}> {}

/**
 * 新会话通知
 */
export interface NewConversationPayload extends WsMessagePayload<'NEW_CONVERSATION', {
  conversationId: number
  userId: number
  username: string
  createdAt: string
}> {}

/**
 * 未读数更新通知
 */
export interface UnreadCountUpdatePayload extends WsMessagePayload<'UNREAD_COUNT_UPDATE', {
  conversationId: number
  unreadCount: number
  totalCount: number
}> {}

/**
 * 所有 WebSocket 消息类型的联合类型
 */
export type AnyWsPayload = 
  | PrivateMessagePayload
  | MessageStatusPayload
  | MessageRecallPayload
  | MessageDeletePayload
  | UserOnlineStatusPayload
  | CircleMessagePayload
  | CircleMessageDeletePayload
  | NewConversationPayload
  | UnreadCountUpdatePayload

/**
 * 泛型消息处理器（类型安全版本）
 */
export type TypedWsHandler<T extends AnyWsPayload> = (data: T['data']) => void
