declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// 声明 Vue 核心模块
declare module 'vue' {
  import type { 
    Component, 
    ComponentPublicInstance, 
    ComponentInternalInstance,
    App,
    Plugin
  } from 'vue'
  
  // 导出核心 API
  export function createApp(rootComponent: Component): App
  export function defineComponent(options: any): Component
  export function getCurrentInstance(): ComponentInternalInstance | null
  export function onMounted(hook: () => any): void
  export function onUnmounted(hook: () => any): void
  export function onBeforeMount(hook: () => any): void
  export function onBeforeUnmount(hook: () => any): void
  export function ref<T>(value: T): { value: T }
  export function reactive<T extends object>(target: T): T
  export function computed<T>(getter: () => T): { readonly value: T }
  export function watch(source: any, callback: Function, options?: any): Function
  export function nextTick(callback?: () => void): Promise<void>
  
  // 类型导出
  export type {
    Component,
    ComponentPublicInstance,
    ComponentInternalInstance,
    App,
    Plugin
  }
  
  // 全局属性扩展
  export interface ComponentCustomProperties {
    $config: any
    $project: string
    $toolUtil: any
    $http: any
  }
}

// 声明环境变量类型
declare namespace NodeJS {
  interface ProcessEnv {
    VUE_APP_BASE_API_URL: string
    VUE_APP_BASE_API: string
    BASE_URL: string
    NODE_ENV: 'development' | 'production' | 'test'
  }
}

// 声明 ResizeObserver 扩展
interface Window {
  ResizeObserver: typeof ResizeObserver
}

// 声明 vue-router 模块
declare module 'vue-router' {
  import type { RouteLocationNormalized, NavigationGuardNext } from 'vue-router'
  
  export interface RouteMeta {
    title?: string
    [key: string]: any
  }
  
  export type RouteRecordRaw = import('vue-router').RouteRecordRaw
  export type Router = import('vue-router').Router
  
  export function createRouter(options: any): Router
  export function createWebHashHistory(base?: string): any
  export function createWebHistory(base?: string): any
  export function useRoute(): RouteLocationNormalized
  export function useRouter(): Router
}
