/**
 * WebSocket 日志级别
 */
export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  NONE = 4
}

/**
 * 日志配置
 */
interface LoggerConfig {
  level: LogLevel
  prefix: string
  enableColors?: boolean
}

/**
 * WebSocket 专用日志工具
 */
class WebSocketLogger {
  private config: LoggerConfig

  constructor(config: Partial<LoggerConfig> = {}) {
    const isDevelopment = process.env.NODE_ENV === 'development'
    
    this.config = {
      level: isDevelopment ? LogLevel.DEBUG : LogLevel.ERROR,
      prefix: '[WebSocket]',
      enableColors: true,
      ...config
    }
  }

  private formatMessage(level: string, message: string): string {
    const timestamp = new Date().toLocaleTimeString()
    return `${this.config.prefix} ${level} [${timestamp}] ${message}`
  }

  debug(message: string, data?: any): void {
    if (this.config.level > LogLevel.DEBUG) return
    this.log('🔍 DEBUG', message, data, 'color: #3498db')
  }

  info(message: string, data?: any): void {
    if (this.config.level > LogLevel.INFO) return
    this.log('ℹ️ INFO', message, data, 'color: #2ecc71')
  }

  warn(message: string, data?: any): void {
    if (this.config.level > LogLevel.WARN) return
    this.log('⚠️ WARN', message, data, 'color: #f39c12')
  }

  error(message: string, data?: any): void {
    if (this.config.level > LogLevel.ERROR) return
    this.log('❌ ERROR', message, data, 'color: #e74c3c')
  }

  private log(level: string, message: string, data?: any, color?: string): void {
    const formatted = this.formatMessage(level, message)
    
    if (this.config.enableColors && color) {
      console.log(`%c${formatted}`, color, data || '')
    } else {
      console.log(formatted, data || '')
    }
  }

  /**
   * 记录消息订阅
   */
  logSubscription(destination: string, messageType: string): void {
    this.info(`订阅 ${messageType}`, { destination })
  }

  /**
   * 记录消息接收
   */
  logMessageReceived(type: string, payloadSize: number): void {
    this.debug(`收到 ${type} 消息`, { size: `${payloadSize} bytes` })
  }

  /**
   * 记录错误
   */
  logError(context: string, error: Error, metadata?: Record<string, any>): void {
    this.error(context, {
      name: error.name,
      message: error.message,
      stack: error.stack,
      ...metadata
    })
  }

  /**
   * 记录连接状态
   */
  logConnectionState(state: 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING'): void {
    const emojis = {
      CONNECTING: '🔄',
      CONNECTED: '✅',
      DISCONNECTED: '❌',
      RECONNECTING: '🔁'
    }
    this.info(`连接状态：${state}`, { emoji: emojis[state] })
  }
}

export const wsLogger = new WebSocketLogger()

/**
 * 设置日志级别（供调试使用）
 */
export function setLogLevel(level: LogLevel): void {
  ;(wsLogger as any).config.level = level
}
