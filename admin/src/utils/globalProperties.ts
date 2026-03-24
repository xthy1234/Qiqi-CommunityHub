// src/utils/globalProperties.ts
import { getCurrentInstance } from 'vue'

/**
 * 获取全局属性代理对象
 * 提供对 $http, $toolUtil, $config 等全局属性的安全访问
 */
export function useGlobalProperties() {
  const instance = getCurrentInstance()
  
  if (!instance) {
// console.warn('无法获取组件实例，请确保在 setup 或生命周期钩子中调用')
    return createProxy()
  }
  
  const globalProperties = instance.appContext.config.globalProperties
  
  // 返回一个代理对象，确保即使访问不存在的属性也不会报错
  return createProxy(globalProperties)
}

/**
 * 创建全局属性的代理对象
 */
function createProxy(globalProperties?: any) {
  return new Proxy({} as any, {
    get(target, prop) {
      if (!globalProperties) {
// console.warn(`尝试访问全局属性 ${String(prop)}，但无法获取 globalProperties`)
        return undefined
      }
      
      const value = globalProperties[prop]
      
      if (value === undefined) {
// console.warn(`全局属性 ${String(prop)} 未定义`)
      }
      
      return value
    }
  })
}

// 导出便捷的访问函数
export const getHttp = () => useGlobalProperties().$http
export const getToolUtil = () => useGlobalProperties().$toolUtil
export const getConfig = () => useGlobalProperties().$config
export const getMenu = () => useGlobalProperties().$menu
export const getProject = () => useGlobalProperties().$project
