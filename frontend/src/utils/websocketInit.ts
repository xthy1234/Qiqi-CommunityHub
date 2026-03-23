
import { initWebSocket } from '@/utils/websocket'
import { wsLogger, LogLevel } from '@/utils/websocketLogger'
import toolUtil from '@/utils/toolUtil'

/**
 * WebSocket 初始化配置
 */
interface WebSocketInitOptions {
  /** WebSocket 服务器地址 */
  url?: string
  
  /** 是否启用调试日志，默认 true（开发环境） */
  debug?: boolean
  
  /** 心跳间隔（毫秒），默认 30000 */
  heartbeatInterval?: number
  
  /** 重连间隔（毫秒），默认 5000 */
  reconnectInterval?: number
  
  /** 最大重连次数，默认 5 */
  maxReconnectAttempts?: number
}

/**
 * 统一初始化 WebSocket
 * @param options 初始化选项
 */
export function initializeWebSocket(options: WebSocketInitOptions = {}): void {
  const {
    url,
    debug = process.env.NODE_ENV === 'development',
    heartbeatInterval = 30000,
    reconnectInterval = 5000,
    maxReconnectAttempts = 5
  } = options

  // 设置日志级别
  if (!debug) {
    ;(wsLogger as any).config.level = LogLevel.ERROR
  }

  // 构建 WebSocket URL
  const wsUrl = url || `ws://${window.location.hostname}:8080/ws`
  
  wsLogger.info('初始化 WebSocket', {
    url: wsUrl,
    debug,
    heartbeatInterval,
    reconnectInterval,
    maxReconnectAttempts
  })

  // 初始化 WebSocket 管理器
  const ws = initWebSocket(wsUrl)
  
  // 自定义配置（如果需要的话，可以扩展 initWebSocket 函数）
  ;(ws as any).config.heartbeatInterval = heartbeatInterval
  ;(ws as any).config.reconnectInterval = reconnectInterval
  ;(ws as any).config.maxReconnectAttempts = maxReconnectAttempts

  wsLogger.info('WebSocket 初始化完成')
}

/**
 * 在应用启动时自动连接 WebSocket
 * 需要在 main.ts 中调用
 */
export async function connectWebSocketOnStartup(options?: WebSocketInitOptions): Promise<void> {
  try {
    // 检查是否已登录
    const token = toolUtil.storageGet('Token')
    const userId = toolUtil.storageGet('UserInfo') ? JSON.parse(toolUtil.storageGet('UserInfo')!).id : null
    
    if (!token || !userId) {
      wsLogger.warn('用户未登录，跳过 WebSocket 初始化')
      return
    }

    // 初始化 WebSocket
    initializeWebSocket(options)
    
    // 连接到服务器
    const ws = await import('@/utils/websocket').then(m => m.getOrCreateWebSocket())
    await ws.connect()
    
    wsLogger.info('WebSocket 连接成功')
  } catch (error) {
    wsLogger.error('WebSocket 初始化失败', { error })
    throw error
  }
}

/**
 * 断开 WebSocket 连接
 */
export function disconnectWebSocket(): void {
  const { getWebSocket } = require('@/utils/websocket')
  const ws = getWebSocket()
  if (ws) {
    ws.close()
    wsLogger.info('WebSocket 已断开')
  }
}

/**
 * 重置 WebSocket（用于登出）
 */
export function resetWebSocket(): void {
  const { resetWebSocketUserId } = require('@/utils/websocket')
  resetWebSocketUserId()
  wsLogger.info('WebSocket 已重置')
}
