import { useGlobalProperties } from './globalProperties'

/**
 * 默认占位图路径
 */
const DEFAULT_PLACEHOLDER = '/placeholder.svg'

/**
 * 获取完整的 URL 地址
 * @param path 相对路径或完整 URL
 * @param baseUrl 基础 URL（可选）
 * @returns 完整的 URL
 */
export const getFullUrl = (path: string, baseUrl?: string): string => {
  if (!path) return DEFAULT_PLACEHOLDER
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  
  // 如果提供了 baseUrl，直接使用
  if (baseUrl) {
    return `${baseUrl}/${path}`
  }
  
  // 如果没有提供 baseUrl，尝试从全局配置获取
  try {
    const appContext = useGlobalProperties()
    if (appContext && appContext.$config && appContext.$config.url) {
      return `${appContext.$config.url}/${path}`
    }
  } catch (error) {
    console.warn('获取全局配置失败，使用相对路径:', error)
  }
  
  // 如果无法获取配置，返回原路径（可能是相对路径）
  return path
}

/**
 * 获取头像 URL
 * @param avatar 头像路径
 * @returns 完整的头像 URL
 */
export const getAvatarUrl = (avatar?: string | null): string => {
  if (!avatar) return DEFAULT_PLACEHOLDER
  return getFullUrl(avatar)
}

/**
 * 获取文章封面 URL
 * @param coverUrl 封面路径
 * @returns 完整的封面 URL
 */
export const getArticleCoverUrl = (coverUrl?: string | null): string => {
  if (!coverUrl) return DEFAULT_PLACEHOLDER
  const firstImage = coverUrl.split(',')[0]
  return getFullUrl(firstImage)
}

/**
 * 获取性别文本
 * @param gender 性别代码 (0: 未知，1: 男，2: 女)
 * @returns 性别描述文本
 */
export const getGenderText = (gender?: number): string => {
  const genderMap: Record<number, string> = {
    0: '未知',
    1: '男',
    2: '女'
  }
  return genderMap[gender ?? 0] || '未知'
}

/**
 * 格式化日期时间
 * @param dateTime 日期时间字符串
 * @returns 格式化后的日期时间字符串
 */
export const formatDateTime = (dateTime?: string): string => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 格式化日期（仅日期部分）
 * @param dateStr 日期字符串
 * @returns 格式化后的日期字符串
 */
export const formatDate = (dateStr?: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

/**
 * 图片加载失败的默认处理
 * @param e 图片错误事件
 */
export const handleImageError = (e: Event): void => {
  const target = e.target as HTMLImageElement
  if (!target) return
  
  // 直接设置为占位图
  target.src = DEFAULT_PLACEHOLDER
}

/**
 * 审核状态文本映射
 * @param status 状态代码
 * @returns 状态描述文本
 */
export const getAuditStatusText = (status: string | number): string => {
  const statusMap: Record<string, string> = {
    '0': '待审核',
    '1': '已发布',
    '2': '审核不通过',
  }
  return statusMap[status] || '未知状态'
}
