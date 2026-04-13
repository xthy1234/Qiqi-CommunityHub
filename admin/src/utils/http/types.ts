// utils/http/types.ts - 类型定义
import type {AxiosInstance, AxiosRequestConfig, AxiosResponse} from 'axios'

/**
 * API 响应数据接口
 */
export interface ApiResponse<T = any> {
  /** 状态码：0 表示成功，其他表示失败 */
  code: number
  /** 响应消息 */
  msg: string
  /** 响应数据 */
  data: T
}

/**
 * HTTP 客户端配置接口
 */
export interface HttpClientConfig extends AxiosRequestConfig {
  /** 请求超时时间（毫秒） */
  timeout?: number
  /** 是否允许携带凭证 */
  withCredentials?: boolean
  /** 基础 URL */
  baseURL?: string
}

/**
 * HTTP 客户端实例接口
 * 注意：由于响应拦截器已经解包了 AxiosResponse，这里直接返回 ApiResponse<T>
 */
export interface HttpClientInstance {
  request: <T = any>(config: AxiosRequestConfig) => Promise<ApiResponse<T>>
  get: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>
  delete: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>
  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>
  instance: AxiosInstance
}
