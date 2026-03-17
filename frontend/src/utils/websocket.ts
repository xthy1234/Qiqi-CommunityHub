import toolUtil from './toolUtil'
import { ElMessage } from 'element-plus'

/**
 * WebSocket 连接状态
 */
export enum WsReadyState {
  CONNECTING = 0,
  OPEN = 1,
  CLOSING = 2,
  CLOSED = 3
}

/**
 * WebSocket 消息类型
 */
export interface WsMessage {
  type: string
  data: any
  timestamp?: number
}

/**
 * WebSocket 配置接口
 */
export interface WsConfig {
  /** WebSocket 服务器地址 */
  url: string
  /** 心跳间隔（毫秒），默认 30000 */
  heartbeatInterval?: number
  /** 重连间隔（毫秒），默认 5000 */
  reconnectInterval?: number
  /** 最大重连次数，默认 5 */
  maxReconnectAttempts?: number
  /** 是否自动重连，默认 true */
  autoReconnect?: boolean
}

/**
 * WebSocket 管理类
 * 提供单例模式，全局唯一 WebSocket 连接
 */
class WebSocketManager {
  private ws: WebSocket | null = null
  private config: WsConfig
  private heartbeatTimer: any = null
  private reconnectTimer: any = null
  private reconnectAttempts = 0
  private messageHandlers: Map<string, Set<(data: any) => void>> = new Map()
  private stateListeners: Set<(state: WsReadyState) => void> = new Set()
  private isManualClose = false

  constructor(config: WsConfig) {
    this.config = {
      url: config.url,
      heartbeatInterval: config.heartbeatInterval || 30000,
      reconnectInterval: config.reconnectInterval || 5000,
      maxReconnectAttempts: config.maxReconnectAttempts || 5,
      autoReconnect: config.autoReconnect !== false
    }
  }

  /**
   * 连接 WebSocket
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        // 如果已有连接，先关闭
        if (this.ws) {
          this.close()
        }

        this.isManualClose = false
        this.ws = new WebSocket(this.config.url)

        this.ws.onopen = () => {
          console.log('WebSocket connected')
          this.reconnectAttempts = 0
          this.notifyStateChange(WsReadyState.OPEN)
          this.startHeartbeat()
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const message: WsMessage = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('WebSocket message parse error:', error)
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error)
          this.notifyStateChange(WsReadyState.CLOSED)
          reject(error)
        }

        this.ws.onclose = (event) => {
          console.log('WebSocket closed:', event.code, event.reason)
          this.stopHeartbeat()
          this.notifyStateChange(WsReadyState.CLOSED)
          
          // 如果是手动关闭，不重连
          if (this.isManualClose) {
            return
          }

          // 自动重连
          if (this.config.autoReconnect && this.reconnectAttempts < this.config.maxReconnectAttempts!) {
            this.scheduleReconnect()
          } else if (this.reconnectAttempts >= this.config.maxReconnectAttempts!) {
            ElMessage.error('WebSocket 连接失败，已超出最大重试次数')
          }
        }
      } catch (error) {
        reject(error)
      }
    })
  }

  /**
   * 发送消息
   */
  send(message: WsMessage): void {
    if (!this.ws || this.ws.readyState !== WsReadyState.OPEN) {
      console.warn('WebSocket not connected, message skipped:', message)
      return
    }

    const messageWithMeta = {
      ...message,
      timestamp: message.timestamp || Date.now()
    }
    this.ws.send(JSON.stringify(messageWithMeta))
  }

  /**
   * 关闭连接
   */
  close(): void {
    this.isManualClose = true
    this.stopHeartbeat()
    this.clearReconnectTimer()
    
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * 注册消息处理器
   * @param messageType 消息类型
   * @param handler 处理函数
   */
  on(messageType: string, handler: (data: any) => void): () => void {
    if (!this.messageHandlers.has(messageType)) {
      this.messageHandlers.set(messageType, new Set())
    }
    this.messageHandlers.get(messageType)!.add(handler)

    // 返回取消订阅函数
    return () => {
      this.off(messageType, handler)
    }
  }

  /**
   * 移除消息处理器
   */
  off(messageType: string, handler: (data: any) => void): void {
    const handlers = this.messageHandlers.get(messageType)
    if (handlers) {
      handlers.delete(handler)
      if (handlers.size === 0) {
        this.messageHandlers.delete(messageType)
      }
    }
  }

  /**
   * 注册连接状态监听器
   */
  onStateChange(listener: (state: WsReadyState) => void): () => void {
    this.stateListeners.add(listener)
    return () => {
      this.stateListeners.delete(listener)
    }
  }

  /**
   * 获取当前连接状态
   */
  getState(): WsReadyState {
    return this.ws ? this.ws.readyState : WsReadyState.CLOSED
  }

  /**
   * 是否已连接
   */
  isConnected(): boolean {
    return this.ws?.readyState === WsReadyState.OPEN
  }

  /**
   * 开始心跳
   */
  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected()) {
        this.send({
          type: 'PING',
          data: null
        })
      }
    }, this.config.heartbeatInterval)
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 调度重连
   */
  private scheduleReconnect(): void {
    this.clearReconnectTimer()
    this.reconnectAttempts++
    
    this.reconnectTimer = setTimeout(() => {
      console.log(`WebSocket reconnecting... (${this.reconnectAttempts}/${this.config.maxReconnectAttempts})`)
      this.connect().catch(console.error)
    }, this.config.reconnectInterval)
  }

  /**
   * 清除重连定时器
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: WsMessage): void {
    // 处理 PONG 响应
    if (message.type === 'PONG') {
      return
    }

    // 分发到对应的处理器
    const handlers = this.messageHandlers.get(message.type)
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(message.data)
        } catch (error) {
          console.error(`Error in WebSocket handler for ${message.type}:`, error)
        }
      })
    }
  }

  /**
   * 通知状态变化
   */
  private notifyStateChange(state: WsReadyState): void {
    this.stateListeners.forEach(listener => {
      try {
        listener(state)
      } catch (error) {
        console.error('Error in WebSocket state listener:', error)
      }
    })
  }
}

// 创建全局单例
let wsManager: WebSocketManager | null = null

/**
 * 初始化 WebSocket
 * @param url WebSocket 服务器地址，例如：ws://localhost:8080/ws
 */
export function initWebSocket(url?: string): WebSocketManager {
  if (wsManager) {
    return wsManager
  }

  const wsUrl = url || `ws://${window.location.host}/ws`
  const token = toolUtil.storageGet('Token')
  
  // 如果有 token，添加到 URL 中
  const finalUrl = token ? `${wsUrl}?token=${encodeURIComponent(token)}` : wsUrl
  
  wsManager = new WebSocketManager({
    url: finalUrl,
    heartbeatInterval: 30000,
    reconnectInterval: 5000,
    maxReconnectAttempts: 5,
    autoReconnect: true
  })

  return wsManager
}

/**
 * 获取 WebSocket 实例
 */
export function getWebSocket(): WebSocketManager | null {
  return wsManager
}

/**
 * 关闭 WebSocket 连接
 */
export function closeWebSocket(): void {
  if (wsManager) {
    wsManager.close()
    wsManager = null
  }
}

export default WebSocketManager
