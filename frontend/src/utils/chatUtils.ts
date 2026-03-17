import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 格式化消息时间
 */
export function formatChatTime(time: string): string {
  const now = dayjs()
  const target = dayjs(time)
  const diff = now.diff(target, 'day')
  
  if (diff === 0) {
    return target.format('HH:mm')
  } else if (diff === 1) {
    return '昨天'
  } else if (diff < 7) {
    return target.format('dddd')
  } else {
    return target.format('YYYY-MM-DD')
  }
}

/**
 * 获取当前登录用户的 ID
 */
export function getCurrentUserId(): number | null {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    return user?.id || null
  }
  return null
}

/**
 * 跳转到关注列表
 */
export function goToFollowing(router: any) {
  const userId = getCurrentUserId()
  if (userId) {
    router.push(`/user/${userId}/following`)
  } else {
    router.push('/index/articleList')
  }
}

/**
 * 跳转到搜索页面
 */
export function goToSearch(router: any) {
  router.push('/index/articleList')
}
