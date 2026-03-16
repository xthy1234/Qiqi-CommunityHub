// src/utils/http.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import router from '../router/index'
import toolUtil from '@/utils/toolUtil'
import { ElMessage } from 'element-plus'

/**
 * API 响应数据接口
 * @template T 响应数据的具体类型
 */
interface ApiResponse<T = any> {
    /** 状态码：0表示成功，其他表示失败 */
    code: number
    /** 响应消息 */
    msg: string
    /** 响应数据 */
    data: T
}

/**
 * HTTP 客户端配置接口
 * 扩展了 Axios 的基础配置
 */
interface HttpClientConfig extends AxiosRequestConfig {
    /** 请求超时时间（毫秒） */
    timeout?: number
    /** 是否允许携带凭证 */
    withCredentials?: boolean
    /** 基础 URL */
    baseURL?: string
}

/**
 * HTTP 客户端类
 * 封装了 axios 实例，提供统一的请求拦截、响应处理和错误处理机制
 */
class HttpClient {
    /** Axios 实例 */
    public axiosInstance: AxiosInstance

    /**
     * 获取 Axios 实例
     * @returns AxiosInstance
     */
    public getAxiosInstance(): AxiosInstance {
        return this.axiosInstance
    }

    /**
     * 构造函数
     * @param config HTTP 客户端配置
     */
    constructor(config: HttpClientConfig) {
        this.axiosInstance = axios.create(config)
        this.initializeInterceptors()
    }

    /**
     * 初始化请求和响应拦截器
     * @private
     */
    private initializeInterceptors(): void {
        // 请求拦截器 - 自动添加认证 Token
        this.axiosInstance.interceptors.request.use(
            (requestConfig: InternalAxiosRequestConfig) => {
                const token = toolUtil.storageGet('Token')
                if (token && requestConfig.headers) {
                    requestConfig.headers['Token'] = token
                }
                return requestConfig
            },
            (error) => {
                return Promise.reject(error)
            }
        )

        // 响应拦截器 - 统一处理响应和错误
        this.axiosInstance.interceptors.response.use(
            (response: AxiosResponse<ApiResponse>) => {
                const { data } = response

                // 处理未授权访问 (401)
                if (data && data.code === 401) {
                    this.handleUnauthorizedAccess(response)
                    return Promise.reject(response)
                }

                // 处理成功响应
                if (data && data.code === 0) {
                    return response
                }

                // 处理业务逻辑错误
                this.handleBusinessException(response)
                return Promise.reject(response)
            },
            (error) => {
                return Promise.reject(error)
            }
        )
    }

    /**
     * 发送请求的通用方法
     * @template T 响应数据类型
     * @param config 请求配置
     * @returns Promise<AxiosResponse<ApiResponse<T>>>
     */
    public request<T = any>(config: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
        return this.axiosInstance.request(config)
    }

    /**
     * 处理未授权访问
     * @param response 响应对象
     * @private
     */
    private handleUnauthorizedAccess(response: AxiosResponse<ApiResponse>): void {
        // 清除本地认证信息
        toolUtil.storageClear()
        
        // 保存当前路由路径，用于登录后跳转
        toolUtil.storageSet('redirectPath', window.history.state.current)
        
        // 显示错误提示
        ElMessage.error(response.data.msg)
        
        // 跳转到登录页
        router.push('/login')
    }

    /**
     * 处理业务逻辑异常
     * @param response 响应对象
     * @private
     */
    private handleBusinessException(response: AxiosResponse<ApiResponse>): void {
        ElMessage.error(response.data.msg)
    }

    /**
     * 发送 GET 请求
     * @template T 响应数据类型
     * @param url 请求地址
     * @param config 请求配置
     * @returns Promise<AxiosResponse<ApiResponse<T>>>
     */
    public get<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
        return this.axiosInstance.get(url, config)
    }

    /**
     * 发送 POST 请求
     * @template T 响应数据类型
     * @param url 请求地址
     * @param data 请求数据
     * @param config 请求配置
     * @returns Promise<AxiosResponse<ApiResponse<T>>>
     */
    public post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
        return this.axiosInstance.post(url, data, config)
    }

    /**
     * 发送 PUT 请求
     * @template T 响应数据类型
     * @param url 请求地址
     * @param data 请求数据
     * @param config 请求配置
     * @returns Promise<AxiosResponse<ApiResponse<T>>>
     */
    public put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
        return this.axiosInstance.put(url, data, config)
    }

    /**
     * 发送 DELETE 请求
     * @template T 响应数据类型
     * @param url 请求地址
     * @param config 请求配置
     * @returns Promise<AxiosResponse<ApiResponse<T>>>
     */
    public delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
        return this.axiosInstance.delete(url, config)
    }

    /**
     * 发送 PATCH 请求
     * @template T 响应数据类型
     * @param url 请求地址
     * @param data 请求数据
     * @param config 请求配置
     * @returns Promise<AxiosResponse<ApiResponse<T>>>
     */
    public patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
        return this.axiosInstance.patch(url, data, config)
    }
}

// 创建 HTTP 客户端实例
const httpClient = new HttpClient({
    timeout: 1000 * 86400, // 24 小时超时
    withCredentials: true, // 允许携带凭证
    baseURL: 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json; charset=utf-8'
    }
})

// 创建一个可调用的函数包装器
const httpCall = <T = any>(config: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> => {
    return httpClient.request(config)
}

// 将方法绑定到包装函数上，保持向后兼容
httpCall.get = httpClient.get.bind(httpClient)
httpCall.post = httpClient.post.bind(httpClient)
httpCall.put = httpClient.put.bind(httpClient)
httpCall.delete = httpClient.delete.bind(httpClient)
httpCall.patch = httpClient.patch.bind(httpClient)

// 导出默认实例以保持向后兼容性
export default httpCall

// 导出类型定义和类，便于其他模块使用
export type { ApiResponse, HttpClientConfig }
export { HttpClient }
