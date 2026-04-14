/**
 * 文章置顶等级工具
 */

export type FeaturedLevel = 0 | 1 | 2 | 3

export const FEATURED_LEVEL_CONFIG: Record<FeaturedLevel, {
  label: string
  type: 'default' | 'info' | 'warning' | 'error'
  icon: string
  priority: number
}> = {
  0: {
    label: '普通',
    type: 'default',
    icon: 'ri:file-line',
    priority: 0
  },
  1: {
    label: '推荐',
    type: 'info',
    icon: 'ri:thumb-up-line',
    priority: 1
  },
  2: {
    label: '热门',
    type: 'warning',
    icon: 'ri:fire-line',
    priority: 2
  },
  3: {
    label: '重要通知',
    type: 'error',
    icon: 'ri:notification-badge-line',
    priority: 3
  }
}

/**
 * 获取置顶等级的配置信息
 */
export function getFeaturedLevelConfig(level: FeaturedLevel) {
  return FEATURED_LEVEL_CONFIG[level] || FEATURED_LEVEL_CONFIG[0]
}

/**
 * 判断文章是否置顶
 */
export function isArticleFeatured(article: { isFeatured?: boolean; featuredLevel?: number }): boolean {
  return article.isFeatured === true && (article.featuredLevel ?? 0) > 0
}

/**
 * 比较两个文章的置顶优先级（用于排序）
 */
export function compareFeaturedPriority(
  a: { isFeatured?: boolean; featuredLevel?: number },
  b: { isFeatured?: boolean; featuredLevel?: number }
): number {
  const levelA = (a.isFeatured ? a.featuredLevel : 0) || 0
  const levelB = (b.isFeatured ? b.featuredLevel : 0) || 0
  return levelB - levelA // 降序：高优先级在前
}
