// src/api/circleWebSocket.ts
// 圈子聊天 WebSocket 相关操作

import type { CircleMessage } from '@/types/circleChat'
import { getWebSocket, initWebSocket } from '@/utils/websocket'

/**
 * 确保 WebSocket 已连接
 */
export function ensureCircleWebSocketConnected(): Promise<void> {
  return new Promise((resolve, reject) => {
    const ws = getWebSocket()

    if (!ws) {
      console.warn('[圈子聊天] WebSocket 实例不存在，正在初始化...')

      try {
        const wsManager = initWebSocket()
        wsManager.connect()
            .then(() => resolve())
            .catch(reject)
      } catch (error) {
        reject(error)
      }
    } else if (ws.isConnected()) {
      resolve()
    } else {
      const client = (ws as any).client

      if (client && client.state === 'CONNECTING') {
        console.log('[圈子聊天] WebSocket 正在连接中，等待连接完成...')

        const checkInterval = setInterval(() => {
          if (ws.isConnected()) {
            clearInterval(checkInterval)
            console.log('[圈子聊天] WebSocket 连接成功')
            resolve()
          }
        }, 100)

        setTimeout(() => {
          clearInterval(checkInterval)
          reject(new Error('WebSocket 连接超时'))
        }, 10000)
      } else {
        console.log('[圈子聊天] WebSocket 未连接，正在连接...')

        ws.connect()
            .then(() => {
              console.log('[圈子聊天] WebSocket 连接成功')
              resolve()
            })
            .catch(reject)
      }
    }
  })
}

/**
 * WebSocket 操作对象
 */
export const circleWebSocket = {
  /**
   * 订阅圈子消息主题
   */
  subscribeCircleMessages(circleId: number, callback: (message: CircleMessage) => void): () => void {
    const ws = getWebSocket()
    if (!ws) {
      console.warn('[圈子 WebSocket] WebSocket 实例不存在')
      return () => {}
    }

    if (!ws.isConnected()) {
      console.warn('[圈子 WebSocket] WebSocket 未连接')
      return () => {}
    }

    const client = (ws as any).client
    if (!client || !client.connected) {
      console.warn('[圈子 WebSocket] STOMP 客户端未连接')
      return () => {}
    }

    const destination = `/topic/circles/${circleId}/messages`

    try {
      const subscription = client.subscribe(destination, (message: any) => {
        try {
          const chatMessage = JSON.parse(message.body) as CircleMessage
          callback(chatMessage)
        } catch (error) {
          console.error('[圈子 WebSocket] 解析圈子消息失败:', error)
        }
      }, {})

      return () => {
        subscription?.unsubscribe()
      }
    } catch (error) {
      console.error('[圈子 WebSocket] 订阅失败:', error)
      return () => {}
    }
  },

  /**
   * 发送圈子消息
   */
  sendCircleMessage(circleId: number, chatMessage: any): void {
    const ws = getWebSocket()

    if (!ws || !ws.isConnected()) {
      console.error('[圈子 WebSocket] WebSocket 未连接，无法发送消息')
      return
    }

    const message = {
      circleId: circleId,
      content: chatMessage.content,
      msgType: chatMessage.msgType,
      extra: chatMessage.extra || {}
    }

    if (typeof message.content !== 'object' || message.content === null) {
      console.error('[圈子 API] content 不是对象:', typeof message.content)
      return
    }

    if (!message.content.type) {
      console.error('[圈子 API] content 缺少 type 字段，这不是有效的 TipTap JSON')
      return
    }

    if (message.content.type !== 'doc') {
      console.error('[圈子 API] content.type 不是 "doc"，而是:', message.content.type)
    }

    const client = (ws as any).client

    if (client && client.connected) {
      try {
        const serializedBody = JSON.stringify(message)

        client.publish({
          destination: '/app/circle-message',
          body: serializedBody
        })
      } catch (error) {
        console.error('[圈子 WebSocket] 消息发送失败:', error)
      }
    } else {
      console.error('[圈子 WebSocket] STOMP 客户端未连接')
    }
  },

  /**
   * 撤回消息
   */
  recallMessage(messageId: number, reason?: string): void {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      console.error('[圈子 WebSocket] WebSocket 未连接，无法撤回消息')
      return
    }

    const request = {
      messageId: messageId,
      reason: reason || ''
    }

    const client = (ws as any).client
    if (client && client.connected) {
      try {
        client.publish({
          destination: '/app/circle-recall-message',
          body: JSON.stringify(request)
        })
      } catch (error) {
        console.error('[圈子 WebSocket] 撤回消息失败:', error)
      }
    } else {
      console.error('[圈子 WebSocket] STOMP 客户端未连接')
    }
  },

  /**
   * 删除消息（仅群主和管理员）
   */
  deleteMessage(messageId: number): void {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
      console.error('[圈子 WebSocket] WebSocket 未连接，无法删除消息')
      return
    }

    const request = {
      messageId: messageId
    }

    const client = (ws as any).client
    if (client && client.connected) {
      try {
        client.publish({
          destination: '/app/circle-delete-message',
          body: JSON.stringify(request)
        })
      } catch (error) {
        console.error('[圈子 WebSocket] 删除消息失败:', error)
      }
    } else {
      console.error('[圈子 WebSocket] STOMP 客户端未连接')
    }
  }
}

export default circleWebSocket
