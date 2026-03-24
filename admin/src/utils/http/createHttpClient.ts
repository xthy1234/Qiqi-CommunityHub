// utils/http/createHttpClient.ts - 工厂函数
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { setupInterceptors } from './interceptors'
import type {ApiResponse, HttpClientConfig, HttpClientInstance} from './types'

/**
 * 创建 HTTP 客户端实例（工厂函数 - Composition API 风格）
 * @param config HTTP 客户端配置
 * @returns HTTP 客户端实例
 */
export function createHttpClient(config: HttpClientConfig): HttpClientInstance {
  // 创建 Axios 实例
  const instance: AxiosInstance = axios.create({
    baseURL: config.baseURL || 'http://localhost:8080',
    timeout: config.timeout || 30000, // 默认 30 秒，不再是 24 小时！
    withCredentials: config.withCredentials ?? true,
    headers: {
      'Content-Type': 'application/json; charset=utf-8'
    },
    ...config
  })

  // 设置拦截器
  setupInterceptors(instance)

  // 返回封装后的 API
  return {
    instance,
    
    request<T = any>(requestConfig: AxiosRequestConfig) {
      return instance.request<ApiResponse<T>>(requestConfig)
    },
    
    get<T = any>(url: string, requestConfig?: AxiosRequestConfig) {
      return instance.get<ApiResponse<T>>(url, requestConfig)
    },
    
    post<T = any>(url: string, data?: any, requestConfig?: AxiosRequestConfig) {
      return instance.post<ApiResponse<T>>(url, data, requestConfig)
    },
    
    put<T = any>(url: string, data?: any, requestConfig?: AxiosRequestConfig) {
      return instance.put<ApiResponse<T>>(url, data, requestConfig)
    },
    
    delete<T = any>(url: string, requestConfig?: AxiosRequestConfig) {
      return instance.delete<ApiResponse<T>>(url, requestConfig)
    },
    
    patch<T = any>(url: string, data?: any, requestConfig?: AxiosRequestConfig) {
      return instance.patch<ApiResponse<T>>(url, data, requestConfig)
    }
  }
}
