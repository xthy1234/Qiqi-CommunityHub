// src/utils/http.ts
// 重构为 Composition API 风格 - 转发到新的 HTTP 客户端模块
import newHttpClient, { createHttpClient } from './http/index'
import type { ApiResponse, HttpClientConfig, HttpClientInstance } from './http/index'

// 默认导出（保持向后兼容）
export default newHttpClient

// 导出工厂函数
export { createHttpClient }

// 导出类型
export type { ApiResponse, HttpClientConfig, HttpClientInstance }

// 为了完全兼容旧代码，保留 HttpClient 类（但内部使用新实现）
class HttpClient {
  private client: HttpClientInstance
  
  constructor(config: HttpClientConfig) {
    this.client = createHttpClient(config)
  }
  
  getAxiosInstance() {
    return this.client.instance
  }
  
  request<T = any>(config: any) {
    return this.client.request<T>(config)
  }
  
  get<T = any>(url: string, config?: any) {
    return this.client.get<T>(url, config)
  }
  
  post<T = any>(url: string, data?: any, config?: any) {
    return this.client.post<T>(url, data, config)
  }
  
  put<T = any>(url: string, data?: any, config?: any) {
    return this.client.put<T>(url, data, config)
  }
  
  delete<T = any>(url: string, config?: any) {
    return this.client.delete<T>(url, config)
  }
  
  patch<T = any>(url: string, data?: any, config?: any) {
    return this.client.patch<T>(url, data, config)
  }
}

export { HttpClient }
