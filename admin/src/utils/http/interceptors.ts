// utils/http/interceptors.ts - 拦截器配置
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import toolUtil from '@/utils/toolUtil'
import router from '@/router'
import type { ApiResponse } from './types'

/**
 * 设置请求拦截器 - 自动添加 Token
 */
function setupRequestInterceptor(instance: AxiosInstance): void {
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = toolUtil.storageGet('Token')
      if (token && config.headers) {
        config.headers['Token'] = token
      }
      return config
    },
    (error) => {
      return Promise.reject(error)
    }
  )
}

/**
 * 设置响应拦截器 - 统一处理响应和错误
 */
function setupResponseInterceptor(instance: AxiosInstance): void {
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const { data } = response

      // 处理未授权访问 (401)
      if (data && data.code === 401) {
        handleUnauthorizedAccess(response)
        return Promise.reject(response)
      }

      // 处理成功响应
      if (data && data.code === 0) {
        return response
      }

      // 处理业务逻辑错误
      handleBusinessException(response)
      return Promise.reject(response)
    },
    (error) => {
      return Promise.reject(error)
    }
  )
}

/**
 * 处理未授权访问
 */
function handleUnauthorizedAccess(response: AxiosResponse<ApiResponse>): void {
  toolUtil.storageClear()
  
  toolUtil.storageSet('redirectPath', window.history.state.current)
  
  toolUtil.message(response.data.msg, 'error')
  
  router.push('/login')
}

/**
 * 处理业务逻辑异常
 */
function handleBusinessException(response: AxiosResponse<ApiResponse>): void {
  toolUtil.message(response.data.msg, 'error')
}

/**
 * 统一设置所有拦截器
 */
export function setupInterceptors(instance: AxiosInstance): void {
  setupRequestInterceptor(instance)
  setupResponseInterceptor(instance)
}
