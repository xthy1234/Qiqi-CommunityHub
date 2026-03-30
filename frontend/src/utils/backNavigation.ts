import type { Router, RouteLocationNormalizedLoaded } from 'vue-router'

/**
 * 返回操作选项
 */
export interface BackNavigationOptions {
  /** 指定的返回路径（优先级最高） */
  backPath?: string
  /** 降级返回路径（当 backUrl 不存在时使用） */
  fallbackPath?: string
  /** 是否替换当前历史记录（默认 false） */
  replace?: boolean
  /** 是否启用防抖（默认 true） */
  debounce?: boolean
}

/**
 * 跳转配置选项
 */
export interface NavigateWithBackOptions {
  /** 额外的 query 参数 */
  extraQuery?: Record<string, any>
  /** 是否替换当前历史记录（默认 false） */
  replace?: boolean
}

/**
 * 返回路径管理器
 * 用于统一处理页面返回逻辑，解决浏览器历史记录不一致的问题
 */
export class BackNavigationManager {
  // 防抖计时器存储
  private static debounceTimers = new Map<string, NodeJS.Timeout>()

  /**
   * 获取当前页面的编码路径（用于传递给子页面）
   * @param route 当前路由对象
   * @returns 编码后的路径字符串
   */
  static getCurrentBackUrl(route: RouteLocationNormalizedLoaded): string {
    return encodeURIComponent(route.fullPath)
  }

  /**
   * 执行返回操作（增强版）
   * 优先级：backPath > query.backUrl > fallbackPath > router.back()
   * 
   * @param router 路由器实例
   * @param route 当前路由对象
   * @param options 返回选项
   */
  static goBack(
    router: Router,
    route: RouteLocationNormalizedLoaded,
    options?: BackNavigationOptions
  ): void {
    const {
      backPath,
      fallbackPath,
      replace = false,
      debounce: enableDebounce = true
    } = options || {}

    // 生成防抖 key（基于当前路径）
    const debounceKey = `back_${route.fullPath}`

    // 清理之前的定时器
    if (enableDebounce && this.debounceTimers.has(debounceKey)) {
      this.clearDebounce(debounceKey)
    }

    const executeNavigation = () => {
      let targetPath: string | undefined

      // 1. 优先使用指定的 backPath
      if (backPath) {
        targetPath = backPath
      } else {
        // 2. 尝试从 query 参数中获取 backUrl
        const backUrl = route.query.backUrl as string
        if (backUrl) {
          try {
            targetPath = decodeURIComponent(backUrl)
          } catch (error) {
            console.error('❌ [BackNavigation] 解码 backUrl 失败:', error)
          }
        }
      }

      // 3. 使用目标路径或降级路径
      if (targetPath) {
        if (replace) {
          router.replace(targetPath)
        } else {
          router.push(targetPath)
        }
      } else if (fallbackPath) {
        if (replace) {
          router.replace(fallbackPath)
        } else {
          router.push(fallbackPath)
        }
      } else {
        // 4. 最后手段：浏览器后退
        router.back()
      }
    }

    // 应用防抖
    if (enableDebounce) {
      const timer = setTimeout(executeNavigation, 150)
      this.debounceTimers.set(debounceKey, timer)
    } else {
      executeNavigation()
    }
  }

  /**
   * 清除防抖定时器
   * @param key 定时器 key
   */
  private static clearDebounce(key: string): void {
    const timer = this.debounceTimers.get(key)
    if (timer) {
      clearTimeout(timer)
      this.debounceTimers.delete(key)
    }
  }

  /**
   * 构建带返回路径的路由配置
   * @param route 当前路由对象
   * @param targetPath 目标路径
   * @param options 额外选项
   * @returns 包含 backUrl 的路由配置对象
   */
  static buildRouteWithBackUrl(
    route: RouteLocationNormalizedLoaded,
    targetPath: string | { path: string; query?: Record<string, any> },
    options?: NavigateWithBackOptions
  ): { path: string; query: Record<string, any> } {
    const { extraQuery = {}, replace = false } = options || {}
    const backUrl = this.getCurrentBackUrl(route)
    
    if (typeof targetPath === 'string') {
      return {
        path: targetPath,
        query: {
          ...extraQuery,
          backUrl
        }
      }
    } else {
      return {
        path: targetPath.path,
        query: {
          ...(targetPath.query || {}),
          ...extraQuery,
          backUrl
        }
      }
    }
  }

  /**
   * 跳转并设置返回路径（便捷方法）
   * @param router 路由器实例
   * @param route 当前路由对象
   * @param targetPath 目标路径
   * @param options 额外选项
   */
  static navigateWithBackUrl(
    router: Router,
    route: RouteLocationNormalizedLoaded,
    targetPath: string | { path: string; query?: Record<string, any> },
    options?: NavigateWithBackOptions
  ): void {
    const routeConfig = this.buildRouteWithBackUrl(route, targetPath, options)
    
    if (options?.replace) {
      router.replace(routeConfig)
    } else {
      router.push(routeConfig)
    }
  }

  /**
   * 发布/提交成功后返回（增强版）
   * 自动清理草稿缓存并使用 replace 模式返回到来源页面
   * @param router 路由器实例
   * @param route 当前路由对象
   * @param toolUtil 工具类实例（用于清理草稿缓存）
   * @param options 返回选项
   */
  static returnAfterPublish(
    router: Router,
    route: RouteLocationNormalizedLoaded,
    toolUtil?: any,
    options?: Omit<BackNavigationOptions, 'replace'>
  ): void {
    // 清理草稿缓存
    if (toolUtil) {
      toolUtil.storageRemove('currentDraftId')
    }

    // 延迟跳转，确保提示信息显示
    setTimeout(() => {
      this.goBack(router, route, {
        ...options,
        replace: true  // 发布成功后使用 replace，避免回到编辑页
      })
    }, 500)
  }

  /**
   * 取消操作后返回（增强版）
   * 可选择是否清理草稿，支持 replace 模式
   * @param router 路由器实例
   * @param route 当前路由对象
   * @param articleId 文章 ID（可选，用于删除草稿）
   * @param draftApi DraftAPI 实例（可选，用于调用删除接口）
   * @param options 返回选项
   */
  static async returnAfterCancel(
    router: Router,
    route: RouteLocationNormalizedLoaded,
    articleId?: string | number,
    draftApi?: any,
    options?: BackNavigationOptions
  ): Promise<void> {
    // 如果需要删除草稿
    if (articleId && draftApi) {
      try {
        await draftApi.deleteDraft(articleId)
      } catch (error) {
        console.error('❌ [BackNavigation] 删除草稿失败:', error)
      }
    }

    // 返回上一页（默认使用 replace，避免保留编辑页历史）
    this.goBack(router, route, {
      ...options,
      replace: options?.replace ?? true
    })
  }

  /**
   * 回滚操作后返回（增强版）
   * 专门用于版本回滚场景，使用 replace 模式避免保留版本列表历史
   * @param router 路由器实例
   * @param route 当前路由对象
   * @param articleId 文章 ID
   * @param reloadCallback 重新加载数据的回调（可选）
   * @param options 返回选项
   */
  static async returnAfterRollback(
    router: Router,
    route: RouteLocationNormalizedLoaded,
    articleId: string | number,
    reloadCallback?: () => Promise<void>,
    options?: Omit<BackNavigationOptions, 'replace' | 'fallbackPath'>
  ): Promise<void> {
    // 如果需要重新加载数据
    if (reloadCallback) {
      await reloadCallback()
    }

    // 返回文章详情页（使用 replace 替换当前版本列表页）
    this.goBack(router, route, {
      ...options,
      fallbackPath: `/index/articleDetail?id=${articleId}`,
      replace: true  // 回滚后使用 replace，避免用户再回到版本列表
    })
  }

  /**
   * 从通知中心跳转后返回
   * 专门用于通知中心跳转到目标页面，确保能返回通知中心
   * @param router 路由器实例
   * @param route 当前路由对象（通知中心的路由）
   * @param targetPath 目标路径
   * @param options 额外选项
   */
  static navigateFromNotification(
    router: Router,
    route: RouteLocationNormalizedLoaded,
    targetPath: string | { path: string; query?: Record<string, any> },
    options?: NavigateWithBackOptions
  ): void {
    // 强制将通知中心路径作为 backUrl
    const notificationPath = encodeURIComponent(route.fullPath)
    const finalTarget = typeof targetPath === 'string' 
      ? { path: targetPath }
      : { ...targetPath }
    
    this.navigateWithBackUrl(router, route, finalTarget, {
      ...options,
      extraQuery: {
        ...(options?.extraQuery || {}),
        backUrl: notificationPath
      }
    })
  }

  /**
   * 清理所有防抖定时器（组件卸载时调用）
   */
  static cleanup(): void {
    this.debounceTimers.forEach(timer => clearTimeout(timer))
    this.debounceTimers.clear()
  }
}

// 导出便捷函数（组合式 API 风格）
import { useRouter, useRoute } from 'vue-router'

/**
 * 使用返回路径管理（组合式 API）
 * @returns 返回路径管理相关方法
 */
export function useBackNavigation() {
  const router = useRouter()
  const route = useRoute()

  return {
    /**
     * 获取当前返回 URL
     */
    getBackUrl: () => BackNavigationManager.getCurrentBackUrl(route),

    /**
     * 执行返回操作
     */
    goBack: (options?: BackNavigationOptions) => 
      BackNavigationManager.goBack(router, route, options),

    /**
     * 构建带返回路径的路由
     */
    buildRouteWithBackUrl: (
      targetPath: string | { path: string; query?: Record<string, any> },
      options?: NavigateWithBackOptions
    ) => BackNavigationManager.buildRouteWithBackUrl(route, targetPath, options),

    /**
     * 跳转并设置返回路径
     */
    navigateWithBackUrl: (
      targetPath: string | { path: string; query?: Record<string, any> },
      options?: NavigateWithBackOptions
    ) => BackNavigationManager.navigateWithBackUrl(router, route, targetPath, options),

    /**
     * 发布成功后返回
     */
    returnAfterPublish: (toolUtil?: any, options?: Omit<BackNavigationOptions, 'replace'>) =>
      BackNavigationManager.returnAfterPublish(router, route, toolUtil, options),

    /**
     * 取消后返回
     */
    returnAfterCancel: async (
      articleId?: string | number,
      draftApi?: any,
      options?: BackNavigationOptions
    ) => await BackNavigationManager.returnAfterCancel(router, route, articleId, draftApi, options),

    /**
     * 回滚后返回
     */
    returnAfterRollback: async (
      articleId: string | number,
      reloadCallback?: () => Promise<void>,
      options?: Omit<BackNavigationOptions, 'replace' | 'fallbackPath'>
    ) => await BackNavigationManager.returnAfterRollback(router, route, articleId, reloadCallback, options),

    /**
     * 从通知中心跳转
     */
    navigateFromNotification: (
      targetPath: string | { path: string; query?: Record<string, any> },
      options?: NavigateWithBackOptions
    ) => BackNavigationManager.navigateFromNotification(router, route, targetPath, options),

    /**
     * 清理防抖定时器（组件卸载时调用）
     */
    cleanup: () => BackNavigationManager.cleanup()
  }
}

export default BackNavigationManager
