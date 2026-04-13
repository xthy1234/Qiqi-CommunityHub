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
    timeout: config.timeout || 30000,
    withCredentials: config.withCredentials ?? true,
    headers: {
      'Content-Type': 'application/json; charset=utf-8'
    },
    ...config
  })

  // 设置拦截器
  setupInterceptors(instance)

  // 返回封装后的 API
  // 注意：由于拦截器已经将 AxiosResponse<ApiResponse<T>> 解包为 ApiResponse<T>
  // 这里使用类型断言来同步类型系统和实际行为
  return {
    instance,
    
    request<T = any>(requestConfig: AxiosRequestConfig): Promise<ApiResponse<T>> {
      return instance.request(requestConfig) as unknown as Promise<ApiResponse<T>>
    },
    
    get<T = any>(url: string, requestConfig?: AxiosRequestConfig): Promise<ApiResponse<T>> {
      return instance.get(url, requestConfig) as unknown as Promise<ApiResponse<T>>
    },
    
    post<T = any>(url: string, data?: any, requestConfig?: AxiosRequestConfig): Promise<ApiResponse<T>> {
      return instance.post(url, data, requestConfig) as unknown as Promise<ApiResponse<T>>
    },
    
    put<T = any>(url: string, data?: any, requestConfig?: AxiosRequestConfig): Promise<ApiResponse<T>> {
      return instance.put(url, data, requestConfig) as unknown as Promise<ApiResponse<T>>
    },
    
    delete<T = any>(url: string, requestConfig?: AxiosRequestConfig): Promise<ApiResponse<T>> {
      return instance.delete(url, requestConfig) as unknown as Promise<ApiResponse<T>>
    },
    
    patch<T = any>(url: string, data?: any, requestConfig?: AxiosRequestConfig): Promise<ApiResponse<T>> {
      return instance.patch(url, data, requestConfig) as unknown as Promise<ApiResponse<T>>
    }
  }
}
