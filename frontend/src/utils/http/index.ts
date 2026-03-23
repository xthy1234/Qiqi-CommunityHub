// utils/http/index.ts - 主入口（保持向后兼容）
import { createHttpClient } from './createHttpClient'
import type { HttpClientInstance, ApiResponse, HttpClientConfig } from './types'

// 创建默认 HTTP 客户端实例
const defaultHttpClient: HttpClientInstance = createHttpClient({
  baseURL: 'http://localhost:8080',
  timeout: 60000*60, // 30 秒超时
  withCredentials: true
})

// 导出默认实例（保持向后兼容）
export default defaultHttpClient

// 导出工厂函数，供需要的地方使用
export { createHttpClient }

// 导出类型
export type { ApiResponse, HttpClientConfig, HttpClientInstance }
