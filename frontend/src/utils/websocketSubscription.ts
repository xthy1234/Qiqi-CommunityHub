import type { Client, IMessage } from '@stomp/stompjs'

/**
 * 统一的订阅处理器配置
 */
interface SubscriptionConfig<T = any> {
  /** 消息类型标识 */
  messageType: string
  
  /** 订阅目标地址 */
  destination: string
  
  /** 消息处理器 */
  handler: (data: T) => void
  
  /** 错误处理回调（可选） */
  onError?: (error: Error, rawMessage: IMessage) => void
  
  /** 日志前缀（可选） */
  logPrefix?: string
}

/**
 * 创建标准化的 STOMP 订阅
 */
export function createStompSubscription(
  client: Client,
  config: SubscriptionConfig
): () => void {
  const subscription = client.subscribe(config.destination, (message: IMessage) => {
    try {
      const data = JSON.parse(message.body)
      
      // 统一错误处理
      try {
        config.handler(data)
      } catch (handlerError) {
        console.error(
          `[${config.logPrefix || 'WebSocket'}] ${config.messageType} 处理器执行失败:`,
          handlerError
        )
        config.onError?.(handlerError as Error, message)
      }
    } catch (parseError) {
      console.error(
        `[${config.logPrefix || 'WebSocket'}] ${config.messageType} 消息解析失败:`,
        parseError
      )
      console.error('原始数据:', message.body)
      config.onError?.(parseError as Error, message)
    }
  }, {})

  // 返回取消订阅函数
  return () => {
    subscription.unsubscribe()
  }
}

/**
 * 批量创建订阅
 */
export function createBatchSubscriptions(
  client: Client,
  configs: SubscriptionConfig[]
): () => void {
  const unsubscribers = configs.map(config => createStompSubscription(client, config))
  
  return () => {
    unsubscribers.forEach(unsubscribe => unsubscribe())
  }
}
