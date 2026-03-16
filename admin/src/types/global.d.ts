NEW_FILE_CODE
// src/types/global.d.ts

/**
 * HTTP 客户端接口定义
 */
interface HttpClient {
  get<T = any>(url: string, config?: any): Promise<any>
  post<T = any>(url: string, data?: any, config?: any): Promise<any>
  put<T = any>(url: string, data?: any, config?: any): Promise<any>
  delete<T = any>(url: string, config?: any): Promise<any>
}

/**
 * ToolUtil 工具类接口定义
 */
interface ToolUtil {
  message(msg: string, type?: 'success' | 'warning' | 'info' | 'error', close?: () => void): void
  notify(title: string, msg: string, type?: 'success' | 'warning' | 'info' | 'error', close?: () => void): void
  storageSet(key: string, value: string): void
  storageGet(key: string): string
  storageGetObj<T = any>(key: string): T | null
  storageRemove(key: string): void
  storageClear(): void
  isEmail(s: string): boolean
  isMobile(s: string): boolean
  isPhone(s: string): boolean
  isURL(s: string): boolean
  isNumber(s: string): boolean
  isIntNumer(s: string): boolean
  checkIdCard(idcard: string): boolean
  isAuth(tableName: string, key: string): boolean
  getCurDateTime(): string
  getCurDate(): string
  encryptDes(message: string): string
  decryptDes(ciphertext: string): string
  encryptAes(msg: string): string
  decryptAes(msg: string): string
}

/**
 * 应用配置接口定义
 */
interface AppConfig {
  url: string
  name: string
  menuList: Array<{
    name: string
    icon: string
    child: Array<{
      name: string
      url: string
    }>
  }>
}

/**
 * Vue 全局属性扩展
 */
declare module 'vue' {
  export interface ComponentCustomProperties {
    $http: HttpClient
    $toolUtil: ToolUtil
    $config: AppConfig
    $project: string
  }
}

export {}
